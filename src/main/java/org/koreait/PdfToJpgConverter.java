package org.koreait;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.text.PDFTextStripper;

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
    private JButton processImagesButton, processOcrButton, processVectorButton, previewButton, saveTextButton, correctButton, insertToDBButton;
    private JList<ImagePage> imageList;
    private DefaultListModel<ImagePage> listModel;
    private File pdfFile;
    private List<BufferedImage> imagePages = new ArrayList<>();
    private JTextArea ocrResultArea;
    private JScrollPane scrollPane;
    private JLabel dropLabel;

    public PdfToJpgConverter() {
        setTitle("PDF Converter");
        setSize(800, 600); // Increased size to accommodate all components
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        panel = new JPanel();
        panel.setLayout(new BorderLayout());

        dropLabel = new JLabel("Drag and drop a PDF file here", SwingConstants.CENTER);
        dropLabel.setPreferredSize(new Dimension(400, 300)); // Set preferred size for initial label visibility

        // Buttons setup
        processImagesButton = new JButton("PDF to Images");
        processImagesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadPdfImages();
            }
        });

        processOcrButton = new JButton("PDF to Text (OCR)");
        processOcrButton.setEnabled(false);
        processOcrButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performOcrOnSelectedPage();
            }
        });

        processVectorButton = new JButton("PDF to Text (Vector)");
        processVectorButton.setEnabled(false);
        processVectorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performVectorTextExtractionOnSelectedPage();
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

        // Initialize InsertToDBButton
        insertToDBButton = new JButton("Insert To DB");
        insertToDBButton.setEnabled(false);
        insertToDBButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onInsertToDBButtonClick();
            }
        });

        // Adding components
        panel.add(dropLabel, BorderLayout.CENTER); // Initially display the drag-and-drop label
        panel.add(new JScrollPane(imageList), BorderLayout.WEST);

        // Create a panel to hold the buttons horizontally
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER)); // Center align buttons

        buttonPanel.add(processImagesButton);
        buttonPanel.add(processOcrButton);
        buttonPanel.add(processVectorButton);
        buttonPanel.add(previewButton);
        buttonPanel.add(saveTextButton);
        buttonPanel.add(correctButton);
        buttonPanel.add(insertToDBButton);

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
                            processOcrButton.setEnabled(true);
                            processVectorButton.setEnabled(true);
                            loadPdfImages();
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
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

    private void performOcrOnSelectedPage() {
        int selectedIndex = imageList.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < imagePages.size()) {
            BufferedImage image = imagePages.get(selectedIndex);
            performOcrOnImage(image);
        } else {
            JOptionPane.showMessageDialog(this, "No page selected for OCR.");
        }
    }

    private void performVectorTextExtractionOnSelectedPage() {
        int selectedIndex = imageList.getSelectedIndex();
        if (selectedIndex >= 0) {
            try (PDDocument document = PDDocument.load(pdfFile)) {
                PDFTextStripper pdfStripper = new PDFTextStripper();
                pdfStripper.setStartPage(selectedIndex + 1);
                pdfStripper.setEndPage(selectedIndex + 1);
                String pageText = pdfStripper.getText(document);

                // JTextArea에 텍스트 설정
                ocrResultArea.setText(pageText);
                scrollPane.setVisible(true); // ScrollPane 표시
                revalidate();
                repaint();

                // 교정 버튼과 텍스트 저장 버튼, DB에 보내기 버튼 활성화
                correctButton.setEnabled(true);
                saveTextButton.setEnabled(true);
                insertToDBButton.setEnabled(true);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error processing PDF file");
            }
        } else {
            JOptionPane.showMessageDialog(this, "No page selected for vector text extraction.");
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

            // 페이지 세그멘테이션 모드 설정
            tesseract.setPageSegMode(1);

            System.out.println("Performing OCR on the image...");
            String result = tesseract.doOCR(image);
            // System.out.println("OCR result: " + result);

            // Update the JTextArea with OCR result
            ocrResultArea.setText(result);
            scrollPane.setVisible(true); // Show scroll pane with OCR result
            revalidate();
            repaint();
            // Enable the correction and save buttons
            correctButton.setEnabled(true);
            saveTextButton.setEnabled(true);
            insertToDBButton.setEnabled(true);
        } catch (TesseractException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "OCR error: " + e.getMessage());
        }
    }

    private void showImagePreview(BufferedImage image) {
        JFrame previewFrame = new JFrame("Image Preview");
        previewFrame.setSize(image.getWidth(), image.getHeight());
        previewFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JLabel imageLabel = new JLabel(new ImageIcon(image));
        previewFrame.add(new JScrollPane(imageLabel));
        previewFrame.setVisible(true);
    }

    private void saveOcrResult() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(ocrResultArea.getText());
                JOptionPane.showMessageDialog(this, "Text saved successfully");
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error saving text file");
            }
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

    private void onInsertToDBButtonClick() {
        // 선택된 텍스트 가져오기
        String selectedText = getSelectedText();
        if (selectedText != null && !selectedText.isEmpty()) {
            MakeJLPTQuery.doMakeJLPTQuery(selectedText);
        } else {
            JOptionPane.showMessageDialog(this, "No text selected for insertion.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PdfToJpgConverter frame = new PdfToJpgConverter();
            frame.setVisible(true);
        });
    }

    private class ImagePage {
        private BufferedImage image;
        private int pageNumber;

        public ImagePage(BufferedImage image, int pageNumber) {
            this.image = image;
            this.pageNumber = pageNumber;
        }

        public BufferedImage getImage() {
            return image;
        }

        public int getPageNumber() {
            return pageNumber;
        }
    }

    private class ImageCellRenderer extends JLabel implements ListCellRenderer<ImagePage> {
        @Override
        public Component getListCellRendererComponent(JList<? extends ImagePage> list, ImagePage value, int index, boolean isSelected, boolean cellHasFocus) {
            setIcon(new ImageIcon(value.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH)));
            setText("Page " + value.getPageNumber());
            if (isSelected) {
                setBackground(Color.LIGHT_GRAY);
                setOpaque(true);
            } else {
                setBackground(Color.WHITE);
                setOpaque(false);
            }
            return this;
        }
    }
}
