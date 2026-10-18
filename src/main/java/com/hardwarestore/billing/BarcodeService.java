package com.hardwarestore.billing;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

@Service
public class BarcodeService {

    public byte[] generateCode128(String value) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 8);

            BitMatrix matrix = new Code128Writer()
                    .encode(value, BarcodeFormat.CODE_128, 420, 100, hints);

            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Could not generate invoice barcode.", ex);
        }
    }
}