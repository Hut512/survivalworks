package de.survivalworkers.core.engine.graphics.vertex;

import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import static org.lwjgl.vulkan.VK10.*;

public class VertexBufferStruct extends VertexInputInfo{
    private static final int NUM_ATTRIB = 2;
    private static final int POS_COMP = 3;
    private static final int TEX_COMP = 2;

    private final VkVertexInputAttributeDescription.Buffer viAttrib;
    private final VkVertexInputBindingDescription.Buffer viDesc;

    public VertexBufferStruct(){
        viAttrib = VkVertexInputAttributeDescription.calloc(NUM_ATTRIB);
        viDesc = VkVertexInputBindingDescription.calloc(1);
        stateInfo = VkPipelineVertexInputStateCreateInfo.calloc();

        int i = 0;
        viAttrib.get(i).binding(0).location(i).format(VK_FORMAT_R32G32B32_SFLOAT).offset(0);
        i++;
        viAttrib.get(i).binding(0).location(i).format(VK_FORMAT_R32G32_SFLOAT).offset(POS_COMP * 4);

        viDesc.get(0).binding(0).stride(POS_COMP * 4 + TEX_COMP * 4).inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

        stateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO).pVertexBindingDescriptions(viDesc).pVertexAttributeDescriptions(viAttrib);
    }

    @Override
    public void close() {
        super.close();
        viAttrib.free();
        viDesc.free();
    }
}
