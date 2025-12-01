package AssetManagement.AssetManagement.util;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

import java.io.ByteArrayOutputStream;

public class AssetCodeUtil {

    public static byte[] generateQrPng(String text, int size) throws Exception {
        BitMatrix matrix = new MultiFormatWriter()
                .encode(text, BarcodeFormat.QR_CODE, size, size);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    public static byte[] generateBarcodePng(String text, int width, int height) throws Exception {
        Code128Writer writer = new Code128Writer();
        BitMatrix matrix = writer.encode(text, BarcodeFormat.CODE_128, width, height);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }
}
