package de.survivalworkers.core.client.engine.vk;

import de.survivalworkers.core.client.engine.vk.pipeline.Pipeline;
import de.survivalworkers.core.client.engine.vk.pipeline.PipelineCache;
import de.survivalworkers.core.client.engine.vk.rendering.*;
import de.survivalworkers.core.client.engine.vk.rendering.Queue;
import de.survivalworkers.core.client.engine.vk.scene.Entity;
import de.survivalworkers.core.client.engine.vk.scene.Scene;
import de.survivalworkers.core.client.engine.vk.shaders.ShaderProgram;
import de.survivalworkers.core.client.engine.vk.vertex.Model;
import de.survivalworkers.core.client.engine.vk.vertex.Texture;
import de.survivalworkers.core.client.engine.vk.vertex.VertexBufferStruct;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

public class ForwardRenderActivity {
    public static final String VERTEX_SHADER = "core/src/main/resources/shader/vertex.vert";
    public static final String FRAGMENT_SHADER = "core/src/main/resources/shader/fragment.frag";

    private final Device device;
    private final CommandBuffer[] commandBuffers;
    private final Fence[] fences;
    private final ShaderProgram shaderProgram;
    private final int materialSize;
    private final Pipeline pipeLine;
    private final PipelineCache pipelineCache;
    private final RenderPass renderPass;
    private final Scene scene;

    private Attachment[] depthAttachments;
    private DescriptorPool descriptorPool;
    private DescriptorSetLayout[] descriptorSetLayouts;
    private Map<String, TextureDescriptorSet> descriptorSetMap;
    private FrameBuffer[] frameBuffers;
    private DescriptorSetLayout.DynUniformDescriptorSetLayout descriptorSetLayout;
    private DescriptorSet.UniformDescriptorSet projMatrixDescriptorSet;
    private Buffer projMatrixUniform;
    private SwapChain swapChain;
    private DescriptorSetLayout.SamplerDescriptorSetLayout textureDescriptorSetLayout;
    private TextureSampler textureSampler;
    private DescriptorSetLayout.UniformDescriptorSetLayout uniformDescriptorSetLayout;
    private DescriptorSet.UniformDescriptorSet[] viewMatricesDescriptorSets;
    private Buffer[] viewMatricesBuffer;
    private Buffer materialBuffer;
    private DescriptorSet.DynUniformDescriptorSet materialsDescriptorSet;

    public ForwardRenderActivity(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache, Scene scene) {
        this.swapChain = swapChain;
        this.pipelineCache = pipelineCache;
        this.scene = scene;
        device = swapChain.getDevice();

        int numImages = swapChain.getImageViews().length;
        materialSize = calcMaterialsUniformSize();
        createDepthImages();
        renderPass = new RenderPass(swapChain, depthAttachments[0].getImage().getFormat());
        createFrameBuffers();

        shaderProgram = new ShaderProgram(device,new ShaderProgram.ShaderModuleData[]{new ShaderProgram.ShaderModuleData(VK13.VK_SHADER_STAGE_VERTEX_BIT,VERTEX_SHADER + ".spv"),
                new ShaderProgram.ShaderModuleData(VK13.VK_SHADER_STAGE_FRAGMENT_BIT,FRAGMENT_SHADER + ".spv")});
        createDescriptorSets(numImages);

        createDescriptorSets(numImages);

        Pipeline.PipeLineCreateInfo pipeLineCreationInfo = new Pipeline.PipeLineCreateInfo(renderPass.getRenderPass(), shaderProgram, 1, true,  64, new VertexBufferStruct(), descriptorSetLayouts,true);
        pipeLine = new Pipeline(this.pipelineCache, pipeLineCreationInfo);
        pipeLineCreationInfo.delete();

        commandBuffers = new CommandBuffer[numImages];
        fences = new Fence[numImages];

        for (int i = 0; i < numImages; i++) {
            commandBuffers[i] = new CommandBuffer(commandPool, true, false);
            fences[i] = new Fence(device, true);
        }

        projMatrixUniform.copyMatrixToBuffer(scene.getProjection().getProjectionMatrix());
    }

    private int calcMaterialsUniformSize() {
        PhysicalDevice physDevice = device.getPhysicalDevice();
        long minUboAlignment = physDevice.getPhysicalDeviceProperties().limits().minUniformBufferOffsetAlignment();
        long mult = (144) / minUboAlignment + 1;
        return (int) (mult * minUboAlignment);
    }

    public void delete() {
        materialBuffer.delete();
        Arrays.stream(viewMatricesBuffer).forEach(Buffer::delete);
        projMatrixUniform.delete();
        textureSampler.delete();
        descriptorPool.delete();
        pipeLine.delete();
        uniformDescriptorSetLayout.delete();
        textureDescriptorSetLayout.delete();
        descriptorSetLayout.delete();
        Arrays.stream(depthAttachments).forEach(Attachment::delete);
        shaderProgram.delete();
        Arrays.stream(frameBuffers).forEach(FrameBuffer::delete);
        renderPass.delete();
        Arrays.stream(commandBuffers).forEach(CommandBuffer::delete);
        Arrays.stream(fences).forEach(Fence::delete);
    }

    private void createDepthImages() {
        int numImages = swapChain.getNumImages();
        VkExtent2D swapChainExtent = swapChain.getSwapChainExtent();
        depthAttachments = new Attachment[numImages];
        for (int i = 0; i < numImages; i++) {
            depthAttachments[i] = new Attachment(device, swapChainExtent.width(), swapChainExtent.height(), VK_FORMAT_D32_SFLOAT, VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);
        }
    }

    private void createDescriptorSets(int numImages) {
        uniformDescriptorSetLayout = new DescriptorSetLayout.UniformDescriptorSetLayout(device, 0, VK_SHADER_STAGE_VERTEX_BIT);
        textureDescriptorSetLayout = new DescriptorSetLayout.SamplerDescriptorSetLayout(device, 0, VK_SHADER_STAGE_FRAGMENT_BIT);
        descriptorSetLayout = new DescriptorSetLayout.DynUniformDescriptorSetLayout(device, 0, VK_SHADER_STAGE_FRAGMENT_BIT);
        descriptorSetLayouts = new DescriptorSetLayout[]{uniformDescriptorSetLayout, uniformDescriptorSetLayout, textureDescriptorSetLayout, descriptorSetLayout,
        };

        List<DescriptorPool.DescriptorTypeCount> descriptorTypeCounts = new ArrayList<>();
        descriptorTypeCounts.add(new DescriptorPool.DescriptorTypeCount(swapChain.getNumImages() + 1, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER));
        descriptorTypeCounts.add(new DescriptorPool.DescriptorTypeCount(10, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER));
        descriptorTypeCounts.add(new DescriptorPool.DescriptorTypeCount(1, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC));
        descriptorPool = new DescriptorPool(device, descriptorTypeCounts);
        descriptorSetMap = new HashMap<>();
        textureSampler = new TextureSampler(device, 1);
        projMatrixUniform = new Buffer(device, 64, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
        projMatrixDescriptorSet = new DescriptorSet.UniformDescriptorSet(descriptorPool, uniformDescriptorSetLayout, projMatrixUniform, 0);

        viewMatricesDescriptorSets = new DescriptorSet.UniformDescriptorSet[numImages];
        viewMatricesBuffer = new Buffer[numImages];
        materialBuffer = new Buffer(device, (long) materialSize * 10, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
        materialsDescriptorSet = new DescriptorSet.DynUniformDescriptorSet(descriptorPool, descriptorSetLayout,
                materialBuffer, 0, materialSize);
        for (int i = 0; i < numImages; i++) {
            viewMatricesBuffer[i] = new Buffer(device, 64, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
            viewMatricesDescriptorSets[i] = new DescriptorSet.UniformDescriptorSet(descriptorPool, uniformDescriptorSetLayout, viewMatricesBuffer[i], 0);
        }
    }

    private void createFrameBuffers() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExtent2D swapChainExtent = swapChain.getSwapChainExtent();
            ImageView[] imageViews = swapChain.getImageViews();
            int numImages = imageViews.length;

            LongBuffer pAttachments = stack.mallocLong(2);
            frameBuffers = new FrameBuffer[numImages];
            for (int i = 0; i < numImages; i++) {
                pAttachments.put(0, imageViews[i].getImgView());
                pAttachments.put(1, depthAttachments[i].getImageView().getImgView());
                frameBuffers[i] = new FrameBuffer(device, swapChainExtent.width(), swapChainExtent.height(), pAttachments, renderPass.getRenderPass());
            }
        }
    }

    public void recordCommandBuffer(List<Model> vulkanModelList) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExtent2D swapChainExtent = swapChain.getSwapChainExtent();
            int width = swapChainExtent.width();
            int height = swapChainExtent.height();
            int idx = swapChain.getCurrentFrame();

            Fence fence = fences[idx];
            CommandBuffer commandBuffer = commandBuffers[idx];
            FrameBuffer frameBuffer = frameBuffers[idx];

            fence.fenceWait();
            fence.reset();

            commandBuffer.reset();
            VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
            clearValues.apply(0, v -> v.color().float32(0, 0.5f).float32(1, 0.7f).float32(2, 0.9f).float32(3, 1));
            clearValues.apply(1, v -> v.depthStencil().depth(1.0f));

            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO).renderPass(renderPass.getRenderPass()).pClearValues(clearValues).renderArea(a -> a.extent().set(width, height)).
                    framebuffer(frameBuffer.getFrameBuffer());

            commandBuffer.beginRec();
            VkCommandBuffer cmdHandle = commandBuffer.getCmdBuf();
            vkCmdBeginRenderPass(cmdHandle, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);

            vkCmdBindPipeline(cmdHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeLine.getPipeline());

            VkViewport.Buffer viewport = VkViewport.calloc(1, stack).x(0).y(height).height(-height).width(width).minDepth(0.0f).maxDepth(1.0f);
            vkCmdSetViewport(cmdHandle, 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack).extent(it -> it.width(width).height(height)).offset(it -> it.x(0).y(0));
            vkCmdSetScissor(cmdHandle, 0, scissor);

            LongBuffer descriptorSets = stack.mallocLong(4).put(0, projMatrixDescriptorSet.getDescriptorSet()).put(1, viewMatricesDescriptorSets[idx].getDescriptorSet()).put(3, materialsDescriptorSet.getDescriptorSet());
            viewMatricesBuffer[idx].copyMatrixToBuffer(scene.getCamera().getViewMatrix());

            recordEntities(stack, cmdHandle, descriptorSets, vulkanModelList);

            vkCmdEndRenderPass(cmdHandle);
            commandBuffer.endRec();
        }
    }

    private void recordEntities(MemoryStack stack, VkCommandBuffer cmdHandle, LongBuffer descriptorSets,
                                List<Model> vulkanModelList) {
        LongBuffer offsets = stack.mallocLong(1);
        offsets.put(0, 0L);
        LongBuffer vertexBuffer = stack.mallocLong(1);
        IntBuffer dynDescrSetOffset = stack.callocInt(1);
        int materialCount = 0;
        for (Model vulkanModel : vulkanModelList) {
            String modelId = vulkanModel.getModelId();
            List<Entity> entities = scene.get(modelId);
            if (entities.isEmpty()) {
                materialCount += vulkanModel.getMaterials().size();
                continue;
            }
            for (Model.Material material : vulkanModel.getMaterials()) {
                if (material.meshes().isEmpty()) {
                    materialCount++;
                    continue;
                }

                int materialOffset = materialCount * materialSize;
                dynDescrSetOffset.put(0, materialOffset);
                TextureDescriptorSet textureDescriptorSet = descriptorSetMap.get(material.texture().getFileName());
                descriptorSets.put(2, textureDescriptorSet.getDescriptorSet());

                for (Model.Mesh mesh : material.meshes()) {
                    vertexBuffer.put(0, mesh.verticesBuffer().getBuffer());
                    vkCmdBindVertexBuffers(cmdHandle, 0, vertexBuffer, offsets);
                    vkCmdBindIndexBuffer(cmdHandle, mesh.indicesBuffer().getBuffer(), 0, VK_INDEX_TYPE_UINT32);

                    for (Entity entity : entities) {
                        vkCmdBindDescriptorSets(cmdHandle, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                pipeLine.getPipelineLayout(), 0, descriptorSets, dynDescrSetOffset);

                        pipeLine.setMAtrixAsPushConstant( cmdHandle, entity.getModelMatrix());
                        vkCmdDrawIndexed(cmdHandle, mesh.numIndices(), 1, 0, 0, 0);
                    }
                }
                materialCount++;
            }
        }
    }

    public void registerModels(List<Model> vulkanModelList) {
        device.waitIdle();
        int materialCount = 0;
        for (Model vulkanModel : vulkanModelList) {
            for (Model.Material vulkanMaterial : vulkanModel.getMaterials()) {
                int materialOffset = materialCount * materialSize;
                updateTextureDescriptorSet(vulkanMaterial.texture());
                updateMaterialsBuffer(materialBuffer, vulkanMaterial, materialOffset);
                materialCount++;
            }
        }
    }

    public void resize(SwapChain swapChain) {
        projMatrixUniform.copyMatrixToBuffer(scene.getProjection().getProjectionMatrix());
        this.swapChain = swapChain;
        Arrays.stream(frameBuffers).forEach(FrameBuffer::delete);
        Arrays.stream(depthAttachments).forEach(Attachment::delete);
        createDepthImages();
        createFrameBuffers();
    }

    public void submit(Queue queue) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int idx = swapChain.getCurrentFrame();
            CommandBuffer commandBuffer = commandBuffers[idx];
            Fence currentFence = fences[idx];
            SwapChain.SyncSemaphores syncSemaphores = swapChain.getSyncSemaphoresList()[idx];
            queue.submit(stack.pointers(commandBuffer.getCmdBuf()), stack.longs(syncSemaphores.imgAcquisitionSemaphore().getSemaphore()), stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT), stack.longs(syncSemaphores.renderCompleteSemaphore().
                    getSemaphore()), currentFence);
        }
    }

    private void updateMaterialsBuffer(Buffer vulkanBuffer, Model.Material material, int offset) {
        long mappedMemory = vulkanBuffer.map();
        ByteBuffer materialBuffer = MemoryUtil.memByteBuffer(mappedMemory, (int) vulkanBuffer.getRequestedSize());
        material.diffuseColor().get(offset, materialBuffer);
        vulkanBuffer.unMap();
    }

    private void updateTextureDescriptorSet(Texture texture) {
        String textureFileName = texture.getFileName();
        TextureDescriptorSet textureDescriptorSet = descriptorSetMap.get(textureFileName);
        if (textureDescriptorSet == null) {
            textureDescriptorSet = new TextureDescriptorSet(descriptorPool, textureDescriptorSetLayout, texture, textureSampler, 0);
            descriptorSetMap.put(textureFileName, textureDescriptorSet);
        }
    }
}