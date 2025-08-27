package com.example.skilltestforfullstackdevelopertrainee_apiwatketsawong;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

import java.nio.ByteBuffer;

public class YuvToRgbConverter {
    private final RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Allocation yuvAllocation;
    private Allocation rgbAllocation;

    public YuvToRgbConverter(Context context) {
        rs = RenderScript.create(context);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }

    public void yuvToRgb(Image image, Bitmap outputBitmap) {
        if (yuvAllocation == null || yuvAllocation.getBytesSize() != image.getPlanes()[0].getBuffer().capacity()) {
            Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs))
                    .setX(image.getPlanes()[0].getRowStride() * image.getHeight());
            yuvAllocation = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
            rgbAllocation = Allocation.createFromBitmap(rs, outputBitmap);
        }

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        yuvAllocation.copyFrom(nv21);
        yuvToRgbIntrinsic.setInput(yuvAllocation);
        yuvToRgbIntrinsic.forEach(rgbAllocation);
        rgbAllocation.copyTo(outputBitmap);
    }
}
