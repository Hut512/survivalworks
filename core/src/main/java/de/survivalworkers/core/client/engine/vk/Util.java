package de.survivalworkers.core.client.engine.vk;

import lombok.experimental.UtilityClass;
import java.util.List;

import static org.lwjgl.vulkan.EXTBufferDeviceAddress.VK_ERROR_INVALID_DEVICE_ADDRESS_EXT;
import static org.lwjgl.vulkan.EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT;
import static org.lwjgl.vulkan.EXTFullScreenExclusive.VK_ERROR_FULL_SCREEN_EXCLUSIVE_MODE_LOST_EXT;
import static org.lwjgl.vulkan.EXTImageCompressionControl.VK_ERROR_COMPRESSION_EXHAUSTED_EXT;
import static org.lwjgl.vulkan.EXTShaderObject.VK_ERROR_INCOMPATIBLE_SHADER_BINARY_EXT;
import static org.lwjgl.vulkan.KHRDeferredHostOperations.*;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_NATIVE_WINDOW_IN_USE_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_SURFACE_LOST_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.KHRVideoEncodeQueue.VK_ERROR_INVALID_VIDEO_STD_PARAMETERS_KHR;
import static org.lwjgl.vulkan.KHRVideoQueue.*;
import static org.lwjgl.vulkan.NVGLSLShader.VK_ERROR_INVALID_SHADER_NV;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_ERROR_INVALID_EXTERNAL_HANDLE;
import static org.lwjgl.vulkan.VK11.VK_ERROR_OUT_OF_POOL_MEMORY;
import static org.lwjgl.vulkan.VK12.VK_ERROR_FRAGMENTATION;
import static org.lwjgl.vulkan.VK13.VK_PIPELINE_COMPILE_REQUIRED;

@UtilityClass
public class Util {

    /** This method checks weather a return code is an error and if true throws an Exception
     * @param returnCode the code that is checked
     * @param errorMessage the String that will be prefixed to the
     */
    public void check(int returnCode, String errorMessage) {
        if (returnCode != VK_SUCCESS) {
            throw new RuntimeException(errorMessage + ": " + translateVulkanResult(returnCode));
        }
    }

    /**
     * Translates a Vulkan {@code VkResult} value to a String describing the result.
     *
     * @param result the {@code VkResult} value
     *
     * @return the result description
     */
    public String translateVulkanResult(int result) {
        return switch (result) {
            // Success codes
            case VK_SUCCESS -> "Command successfully completed.";
            case VK_NOT_READY -> "A fence or query has not yet completed.";
            case VK_TIMEOUT -> "A wait operation has not completed in the specified time.";
            case VK_EVENT_SET -> "An event is signaled.";
            case VK_EVENT_RESET -> "An event is unsignaled.";
            case VK_INCOMPLETE -> "A return array was too small for the result.";
            case VK_SUBOPTIMAL_KHR ->
                    "A swapchain no longer matches the surface properties exactly, but can still be used to present to the surface successfully.";
            case VK_THREAD_IDLE_KHR -> "A deferred operation is not complete but there is currently no work for this thread to do at the time of this call.";
            case VK_THREAD_DONE_KHR -> "A deferred operation is not complete but there is no work remaining to assign to additional threads.";
            case VK_OPERATION_DEFERRED_KHR -> "A deferred operation was requested and at least some of the work was deferred.";
            case VK_OPERATION_NOT_DEFERRED_KHR -> "A deferred operation was requested and no operations were deferred.";
            case VK_PIPELINE_COMPILE_REQUIRED -> "A requested pipeline creation would have required compilation, but the application requested compilation to not be performed.";

            // Error codes
            case VK_ERROR_OUT_OF_HOST_MEMORY -> "A host memory allocation has failed.";
            case VK_ERROR_OUT_OF_DEVICE_MEMORY -> "A device memory allocation has failed.";
            case VK_ERROR_INITIALIZATION_FAILED ->
                    "Initialization of an object could not be completed for implementation-specific reasons.";
            case VK_ERROR_DEVICE_LOST -> "The logical or physical device has been lost.";
            case VK_ERROR_MEMORY_MAP_FAILED -> "Mapping of a memory object has failed.";
            case VK_ERROR_LAYER_NOT_PRESENT -> "A requested layer is not present or could not be loaded.";
            case VK_ERROR_EXTENSION_NOT_PRESENT -> "A requested extension is not supported.";
            case VK_ERROR_FEATURE_NOT_PRESENT -> "A requested feature is not supported.";
            case VK_ERROR_INCOMPATIBLE_DRIVER ->
                    "The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons.";
            case VK_ERROR_TOO_MANY_OBJECTS -> "Too many objects of the type have already been created.";
            case VK_ERROR_FORMAT_NOT_SUPPORTED -> "A requested format is not supported on this device.";
            case VK_ERROR_FRAGMENTED_POOL -> "A pool allocation has failed due to fragmentation of the pool’s memory. This must only be returned if no attempt to allocate host or device " +
                    "memory was made to accommodate the new allocation. This should be returned in preference to VK_ERROR_OUT_OF_POOL_MEMORY, but only if the implementation is certain that " +
                    "the pool allocation failure was due to fragmentation.";
            case VK_ERROR_SURFACE_LOST_KHR -> "A surface is no longer available.";
            case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR ->
                    "The requested window is already connected to a VkSurfaceKHR, or to some other non-Vulkan API.";
            case VK_ERROR_OUT_OF_DATE_KHR ->
                    "A surface has changed in such a way that it is no longer compatible with the swapchain, and further presentation requests using the "
                            + "swapchain will fail. Applications must query the new surface properties and recreate their swapchain if they wish to continue"
                            + "presenting to the surface.";
            case VK_ERROR_INCOMPATIBLE_DISPLAY_KHR ->
                    "The display used by a swapchain does not use the same presentable image layout, or is incompatible in a way that prevents sharing an"
                            + " image.";
            case VK_ERROR_INVALID_SHADER_NV -> "One or more shaders failed to compile or link. More details are reported back to the application via VK_EXT_debug_report if enabled.";
            case VK_ERROR_OUT_OF_POOL_MEMORY -> "A pool memory allocation has failed. This must only be returned if no attempt to allocate host or device memory was made to accommodate the " +
                    "new allocation. If the failure was definitely due to fragmentation of the pool, VK_ERROR_FRAGMENTED_POOL should be returned instead.";
            case VK_ERROR_INVALID_EXTERNAL_HANDLE -> "An external handle is not a valid handle of the specified type.";
            case VK_ERROR_FRAGMENTATION -> "A descriptor pool creation has failed due to fragmentation.";
            case VK_ERROR_INVALID_DEVICE_ADDRESS_EXT -> "A buffer creation failed because the requested address is not available.";
            case VK_ERROR_FULL_SCREEN_EXCLUSIVE_MODE_LOST_EXT -> "An operation on a swapchain created with VK_FULL_SCREEN_EXCLUSIVE_APPLICATION_CONTROLLED_EXT failed as it did not have " +
                    "exclusive full-screen access. This may occur due to implementation-dependent reasons, outside of the application’s control.";
            case VK_ERROR_COMPRESSION_EXHAUSTED_EXT -> "An image creation failed because internal resources required for compression are exhausted. This must only be returned when fixed-rate " +
                    "compression is requested.";
            case VK_ERROR_IMAGE_USAGE_NOT_SUPPORTED_KHR -> "The requested VkImageUsageFlags are not supported.";
            case VK_ERROR_VIDEO_PICTURE_LAYOUT_NOT_SUPPORTED_KHR -> "The requested video picture layout is not supported.";
            case VK_ERROR_VIDEO_PROFILE_OPERATION_NOT_SUPPORTED_KHR -> "A video profile operation specified via VkVideoProfileInfoKHR::videoCodecOperation is not supported.";
            case VK_ERROR_VIDEO_PROFILE_FORMAT_NOT_SUPPORTED_KHR -> "Format parameters in a requested VkVideoProfileInfoKHR chain are not supported.";
            case VK_ERROR_VIDEO_PROFILE_CODEC_NOT_SUPPORTED_KHR -> "Codec-specific parameters in a requested VkVideoProfileInfoKHR chain are not supported.";
            case VK_ERROR_VIDEO_STD_VERSION_NOT_SUPPORTED_KHR -> "The specified video Std header version is not supported.";
            case VK_ERROR_INVALID_VIDEO_STD_PARAMETERS_KHR -> "The specified Video Std parameters do not adhere to the syntactic or semantic requirements of the used video compression standard, " +
                    "or values derived from parameters according to the rules defined by the used video compression standard do not adhere to the capabilities of the video compression standard " +
                    "or the implementation.";
            case VK_ERROR_INCOMPATIBLE_SHADER_BINARY_EXT -> "The provided binary shader code is not compatible with this device.";
            case VK_ERROR_UNKNOWN -> " An unknown error has occurred; either the application has provided invalid input, or an implementation failure has occurred.";
            case VK_ERROR_VALIDATION_FAILED_EXT -> "A validation layer found an error.";
            default -> String.format("%s [%d]", "Unknown", result);
        };
    }
    public static float[] toArrayFloat(List<Float> list){
        float[] arr = new float[list.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    public static int[] toArrayInt(List<Integer> list){
        int[] arr = new int[list.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    public int convert(boolean b){
        return b ? 1:0;
    }
}