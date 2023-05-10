package de.survivalworks.core.client.engine.vk.vertex;

import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;

public abstract class VertexInputInfo {
    protected VkPipelineVertexInputStateCreateInfo stateInfo;
    public void delete(){
        stateInfo.free();
    }

    public VkPipelineVertexInputStateCreateInfo getStateInfo() {
        return stateInfo;
    }
}