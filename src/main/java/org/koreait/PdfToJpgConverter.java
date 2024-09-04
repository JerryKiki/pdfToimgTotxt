package org.koreait;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class PdfToJpgConverter extends JFrame {

    private JPanel panel;
    private JButton saveButton;
    private File pdfFile;

    public PdfToJpgConverter() {
        setTitle("PDF to JPEG Converter");
        setSize(400, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        panel = new JPanel();
        panel.setLayout(new BorderLayout());

        saveButton = new JButton("Save JPEGs");
        saveButton.setEnabled(false); // Save 버튼은 파일이 드롭될 때까지 비활성화

        panel.add(new JLabel("Drag and drop a PDF file here"), BorderLayout.CENTER);
        panel.add(saveButton, BorderLayout.SOUTH);

        add(panel);

        // Drop target listener for PDF file
        new DropTarget(panel, new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetDragEvent e) {
                // Optional: Provide visual feedback
            }

            @Override
            public void dragOver(DropTargetDragEvent e) {
                // Optional: Provide visual feedback
            }

            @Override
            public void dropActionChanged(DropTargetDragEvent e) {
                // Optional: Handle action change
            }

            @Override
            public void dragExit(DropTargetEvent e) {
                // Optional: Provide visual feedback
            }

            @Override
            public void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = e.getTransferable();
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty()) {
                            pdfFile = files.get(0);
                            saveButton.setEnabled(true);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Save button action listener
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveJpegs();
            }
        });
    }

    private void saveJpegs() {
        if (pdfFile != null) {
            try (PDDocument document = PDDocument.load(pdfFile)) {
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                int numPages = document.getNumberOfPages();

                for (int pageIndex = 0; pageIndex < numPages; pageIndex++) {
                    BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, 300); // Render the page with 300 DPI
                    File outputFile = new File(pdfFile.getParent(), "page_" + (pageIndex + 1) + ".jpg");
                    ImageIO.write(image, "jpg", outputFile);
                }

                JOptionPane.showMessageDialog(this, "All pages saved as JPEGs in " + pdfFile.getParent());
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error processing PDF file");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PdfToJpgConverter().setVisible(true));
    }
}
