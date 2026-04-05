package com.flipit.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FileTextExtractor {
    private static final int MAX_CHARS = 25_000;

    public static String extract(File file) throws IOException {
        String name = file.getName().toLowerCase();
        String text = "";

        if (name.endsWith(".txt") || name.endsWith(".md")) {
            text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

        } else if (name.endsWith(".pdf")) {
            try (PDDocument doc = Loader.loadPDF(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                text = stripper.getText(doc);
            }

        } else if (name.endsWith(".docx")) {
            try (FileInputStream fis = new FileInputStream(file);
                 XWPFDocument doc = new XWPFDocument(fis);
                 XWPFWordExtractor ex = new XWPFWordExtractor(doc)) {
                text = ex.getText();
            }

        } else if (name.endsWith(".pptx")) {
            StringBuilder sb = new StringBuilder();
            try (FileInputStream fis = new FileInputStream(file);
                 XMLSlideShow ppt = new XMLSlideShow(fis)) {
                for (XSLFSlide slide : ppt.getSlides()) {
                    for (XSLFShape shape : slide.getShapes()) {
                        if (shape instanceof XSLFTextShape) {
                            sb.append(((XSLFTextShape) shape).getText()).append("\n");
                        }
                    }
                    sb.append("\n");
                }
            }
            text = sb.toString();

        } else {
            throw new IOException("Unsupported file type: " + file.getName());
        }

        if (text != null) {
            text = text.trim();
        }

        if (text == null || text.isEmpty()) {
            throw new IOException("No readable text could be extracted from this file. Please ensure the file contains selectable text.");
        }

        if (text.length() > MAX_CHARS) {
            text = text.substring(0, MAX_CHARS) + "\n[... content truncated ...]";
        }

        return text;
    }
}