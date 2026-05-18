package com.example.backend.report.base;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfWriter;

import java.io.OutputStream;

public final class PdfWriterHolder {

    private PdfWriterHolder() {}

    public static void open(Document document, OutputStream out) throws Exception {
        PdfWriter.getInstance(document, out);
        document.open();
    }
}