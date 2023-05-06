package de.survivalworkers.core.client.engine.graphics.rendering;

import de.survivalworkers.core.client.vk.util.VkUtil;
import de.survivalworkers.core.client.vk.device.SWLogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class Fence {
    private final SWLogicalDevice device;
    private final long handle;

    public Fence(SWLogicalDevice device, boolean signaled){
        this.device = device;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkFenceCreateInfo createInfo = VkFenceCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO).flags(signaled ? VK_FENCE_CREATE_SIGNALED_BIT : 0);

            LongBuffer lp = stack.mallocLong(1);
            VkUtil.check(vkCreateFence(device.getHandle(), createInfo, null, lp),"Could not create Fence");
            handle = lp.get(0);
        }
    }

    public void close() {
        vkDestroyFence(device.getHandle(), handle,null);
    }

    public void fenceWait(){
        vkWaitForFences(device.getHandle(), handle, true, Long.MAX_VALUE);
    }

    public long getHandle() {
        return handle;
    }

    public void reset(){
        vkResetFences(device.getHandle(), handle);
    }
}