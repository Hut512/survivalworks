package de.survivalworkers.core.engine.graphics.rendering;

import de.survivalworkers.core.vk.util.VkUtil;
import de.survivalworkers.core.vk.device.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public abstract class DescriptorSetLayout {
    private final LogicalDevice device;
    protected long descriptorLayout;

    protected DescriptorSetLayout(LogicalDevice device){
        this.device = device;
    }

    public void close() {
        vkDestroyDescriptorSetLayout(device.getHandle(),descriptorLayout,null);
    }

    public long getDescriptorLayout() {
        return descriptorLayout;
    }

    public static class SimpleDescriptorSetLayout extends DescriptorSetLayout{
        public SimpleDescriptorSetLayout(LogicalDevice device, int type, int binding, int stage){
            super(device);
            try(MemoryStack stack = MemoryStack.stackPush()) {
                VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.calloc(1,stack);
                layoutBindings.get(0).binding(binding).descriptorType(type).descriptorCount(1).stageFlags(stage);
                VkDescriptorSetLayoutCreateInfo layoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).pBindings(layoutBindings);
                VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).pBindings(layoutBindings);
                LongBuffer lp = stack.mallocLong(1);
                VkUtil.check(vkCreateDescriptorSetLayout(device.getHandle(),layoutInfo,null,lp),"Could not create descriptor Set Layout");
                super.descriptorLayout = lp.get(0);
            }
        }
    }

    public static class SamplerDescriptorSetLayout extends SimpleDescriptorSetLayout{
        public SamplerDescriptorSetLayout(LogicalDevice device, int binding, int stage){
            super(device,VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,binding,stage);
        }
    }

    public static class UniformDescriptorSetLayout extends SimpleDescriptorSetLayout{
        public UniformDescriptorSetLayout(LogicalDevice device, int binding, int stage){
            super(device, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,binding,stage);
        }
    }

    public static class DynUniformDescriptorSetLayout extends SimpleDescriptorSetLayout{
        public DynUniformDescriptorSetLayout(LogicalDevice device, int binding, int stage){
            super(device,VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC,binding,stage);
        }
    }
}
