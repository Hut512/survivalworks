package de.survivalworkers.core.engine.graphics.pipeline;

import de.survivalworkers.core.vk.device.LogicalDevice;
import de.survivalworkers.core.vk.util.VkUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class PipelineCache {
    private final LogicalDevice device;
    private final long pipelineCache;

    public PipelineCache(LogicalDevice device){
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineCacheCreateInfo createInfo = VkPipelineCacheCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO);

            LongBuffer lp = stack.mallocLong(1);
            VkUtil.check(vkCreatePipelineCache(device.getHandle(), createInfo, null, lp), "Error creating pipeline cache");
            pipelineCache = lp.get(0);
        }
    }

    public void close() {
        vkDestroyPipelineCache(device.getHandle(), pipelineCache, null);
    }

    public LogicalDevice getDevice() {
        return device;
    }

    public long getPipelineCache() {
        return pipelineCache;
    }
}
