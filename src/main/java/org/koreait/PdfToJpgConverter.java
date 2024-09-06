package org.koreait;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfToJpgConverter extends JFrame {

    private JPanel panel;
    private JButton processButton, previewButton, saveTextButton, correctButton;
    private JList<ImagePage> imageList;
    private DefaultListModel<ImagePage> listModel;
    private File pdfFile;
    private List<BufferedImage> imagePages = new ArrayList<>();
    private JTextArea ocrResultArea;
    private JScrollPane scrollPane;
    private JLabel dropLabel;

    public PdfToJpgConverter() {
        setTitle("PDF to JPEG Converter");
        setSize(800, 600); // Increased size to accommodate all components
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        panel = new JPanel();
        panel.setLayout(new BorderLayout());

        dropLabel = new JLabel("Drag and drop a PDF file here", SwingConstants.CENTER);
        dropLabel.setPreferredSize(new Dimension(400, 300)); // Set preferred size for initial label visibility

        // Buttons setup
        processButton = new JButton("Process OCR");
        processButton.setEnabled(false);
        processButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = imageList.getSelectedIndex();
                if (selectedIndex >= 0) {
                    performOcrOnImage(imagePages.get(selectedIndex));
                }
            }
        });

        previewButton = new JButton("Preview Image");
        previewButton.setEnabled(false);

        saveTextButton = new JButton("Save Text");
        saveTextButton.setEnabled(false);

        // List setup
        listModel = new DefaultListModel<>();
        imageList = new JList<>(listModel);
        imageList.setCellRenderer(new ImageCellRenderer());

        // JTextArea and JScrollPane initialization
        ocrResultArea = new JTextArea();
        ocrResultArea.setLineWrap(true);
        ocrResultArea.setWrapStyleWord(true);
        ocrResultArea.setEditable(true);

        scrollPane = new JScrollPane(ocrResultArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVisible(false);

        // Initialize correctButton
        correctButton = new JButton("Correct Text");
        correctButton.setEnabled(false);
        correctButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCorrectButtonClick();
            }
        });

        // Adding components
        panel.add(dropLabel, BorderLayout.CENTER); // Initially display the drag-and-drop label
        panel.add(new JScrollPane(imageList), BorderLayout.WEST);

        // Create a panel to hold the buttons horizontally
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER)); // Center align buttons

        buttonPanel.add(processButton);
        buttonPanel.add(previewButton);
        buttonPanel.add(saveTextButton);
        buttonPanel.add(correctButton);

        panel.add(buttonPanel, BorderLayout.SOUTH); // Add the button panel to the bottom

        // Add a placeholder panel for the center area to dynamically switch between components
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);

        add(panel);

        // Drop target listener for PDF file
        new DropTarget(panel, new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetDragEvent e) {
            }

            @Override
            public void dragOver(DropTargetDragEvent e) {
            }

            @Override
            public void dropActionChanged(DropTargetDragEvent e) {
            }

            @Override
            public void dragExit(DropTargetEvent e) {
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
                            loadPdfImages();
                            processButton.setEnabled(true);
                            previewButton.setEnabled(true);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Process selected image for OCR
        processButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = imageList.getSelectedIndex();
                if (selectedIndex >= 0) {
                    performOcrOnImage(imagePages.get(selectedIndex));
                    scrollPane.setVisible(true);
                    saveTextButton.setEnabled(true);
                }
            }
        });

        // Preview selected image
        previewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = imageList.getSelectedIndex();
                if (selectedIndex >= 0) {
                    showImagePreview(imagePages.get(selectedIndex));
                }
            }
        });

        // Save OCR result as text file
        saveTextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveOcrResult();
            }
        });
    }

    // Load PDF pages into image list
    private void loadPdfImages() {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int numPages = document.getNumberOfPages();
            listModel.clear();
            imagePages.clear();

            for (int pageIndex = 0; pageIndex < numPages; pageIndex++) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, 150); // 150 DPI
                imagePages.add(image);
                listModel.addElement(new ImagePage(image, pageIndex + 1));
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error processing PDF file");
        }
    }

    private void performOcrOnImage(BufferedImage image) {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("C:\\Users\\admin\\IdeaProjects\\pdfToimgTotxt\\Tesseract-OCR\\tessdata");

        // 언어 설정
        tesseract.setLanguage("jpn+jpn_vert");

        // Set OCR Engine Mode to LSTM only (if supported by your Tess4J version)
        try {
            // LSTM 엔진 사용 (1: OEM_LSTM_ONLY)
            tesseract.setOcrEngineMode(1);

            //페이지 세그멘테이션 모드 설정
            tesseract.setPageSegMode(1);

            System.out.println("Performing OCR on the image...");
            String result = tesseract.doOCR(image);
            //System.out.println("OCR result: " + result);

            // Update the JTextArea with OCR result
            ocrResultArea.setText(result);
            scrollPane.setVisible(true); // Show scroll pane with OCR result
            revalidate();
            repaint();

            // Enable the correctButton after OCR processing
            correctButton.setEnabled(true);

        } catch (TesseractException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "OCR Error: " + e.getMessage());
        }
    }

    // Show image preview in a new window
    private void showImagePreview(BufferedImage image) {
        JFrame previewFrame = new JFrame("Image Preview");
        previewFrame.setSize(600, 600);
        JLabel imageLabel = new JLabel(new ImageIcon(image));
        previewFrame.add(new JScrollPane(imageLabel));
        previewFrame.setVisible(true);
    }

    // Save OCR result to text file
    private void saveOcrResult() {
        try {
            String filePath = pdfFile.getParent() + "\\ocr_result.txt";
            FileWriter writer = new FileWriter(filePath);
            writer.write(ocrResultArea.getText());
            writer.close();
            JOptionPane.showMessageDialog(this, "Text saved as " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Custom class to hold image and its corresponding page number
    private static class ImagePage {
        BufferedImage image;
        int pageNumber;

        ImagePage(BufferedImage image, int pageNumber) {
            this.image = image;
            this.pageNumber = pageNumber;
        }

        @Override
        public String toString() {
            return "Page " + pageNumber;
        }
    }

    // Custom renderer to display images in JList
    private static class ImageCellRenderer extends JLabel implements ListCellRenderer<ImagePage> {
        @Override
        public Component getListCellRendererComponent(JList<? extends ImagePage> list, ImagePage value, int index, boolean isSelected, boolean cellHasFocus) {
            setIcon(new ImageIcon(value.image.getScaledInstance(100, 100, Image.SCALE_SMOOTH))); // Thumbnail size
            setText("Page " + value.pageNumber);
            setOpaque(true);
            setBackground(isSelected ? Color.LIGHT_GRAY : Color.WHITE);
            return this;
        }
    }

    private String getSelectedText() {
        return ocrResultArea.getSelectedText();
    }

    private void onCorrectButtonClick() {
        // 선택된 텍스트 가져오기
        String selectedText = getSelectedText();

        if (selectedText != null && !selectedText.isEmpty()) {
            // 선택된 텍스트 교정
            String correctedText = String.join("\n", applyCustomVerticalToHorizontalLogic(selectedText));
            // 교정된 텍스트로 대체
            replaceSelectedText(correctedText);
        } else {
            JOptionPane.showMessageDialog(this, "No text selected for correction.");
        }
    }

    // 세로로 작성된 텍스트를 가로로 변환하는 로직을 적용한 후, 문장별 리스트로 반환하는 메소드
    private static List<String> applyCustomVerticalToHorizontalLogic(String originalText) {
        // 줄바꿈 문자 기준으로 텍스트를 문장별로 분리
        String[] lines = originalText.split("\\r?\\n"); // 윈도우와 유닉스 줄바꿈 처리

        // List로 변환
        List<String> lineList = new ArrayList<>();
        for (String line : lines) {
            lineList.add(line.trim()); // 각 줄의 앞뒤 공백 제거
        }

        // 가로로 변환하는 추가 로직 (예: 모든 공백 제거)
        List<String> cleanedLines = new ArrayList<>();
        for (String line : lineList) {
            cleanedLines.add(line.replaceAll("\\s+", "")); // 공백 제거
        }

        // 새로운 행을 저장할 리스트 생성
        List<String> newRows = new ArrayList<>();
        List<String> oldRows = new ArrayList<>(cleanedLines);

        while (!oldRows.isEmpty()) {
            // 각 행 처리
            List<String> tempRow = new ArrayList<>(oldRows);
            oldRows.clear();

            // 끝에서부터 시작하여 새로운 행을 빌드
            int maxIndex = tempRow.stream().mapToInt(String::length).max().orElse(0);
            for (int i = 0; i < maxIndex; i++) {
                StringBuilder newRow = new StringBuilder();
                for (String row : tempRow) {
                    if (i < row.length()) {
                        newRow.append(row.charAt(row.length() - 1 - i)); // 오른쪽에서 왼쪽으로 추가
                    }
                }
                if (newRow.length() > 0) {
                    newRows.add(newRow.toString());
                }
            }

            // 처리된 행 제거
            oldRows = tempRow.stream()
                    .filter(row -> row.length() > maxIndex) // 더 긴 행만 유지
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }

        // 새로운 행을 하나의 텍스트로 결합
        return newRows;
    }

    private void replaceSelectedText(String correctedText) {
        // 선택된 텍스트의 시작과 끝 인덱스
        int start = ocrResultArea.getSelectionStart();
        int end = ocrResultArea.getSelectionEnd();

        // 원래 텍스트에서 선택된 부분을 교정된 텍스트로 대체
        ocrResultArea.replaceRange(correctedText, start, end);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PdfToJpgConverter().setVisible(true));
    }
}
