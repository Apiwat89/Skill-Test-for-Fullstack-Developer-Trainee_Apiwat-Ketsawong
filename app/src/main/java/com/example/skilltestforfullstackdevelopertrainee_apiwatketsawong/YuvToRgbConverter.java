package com.example.skilltestforfullstackdevelopertrainee_apiwatketsawong;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;

public class YuvToRgbConverter {
    private final RenderScript rs;
    private final ScriptIntrinsicYuvToRGB script;
    private final Allocation inAlloc;
    private final Allocation outAlloc;

    public YuvToRgbConverter(Context context, int width, int height) {
        rs = RenderScript.create(context);
        script = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        int yuvSize = width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
        inAlloc = Allocation.createSized(rs, Element.U8(rs), yuvSize);
        outAlloc = Allocation.createFromBitmap(rs, Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888));
    }

    public void yuvToRgb(byte[] yuvData, Bitmap output) {
        inAlloc.copyFrom(yuvData);
        script.setInput(inAlloc);
        script.forEach(outAlloc);
        outAlloc.copyTo(output);
    }
}