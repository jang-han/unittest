package com.example.demo.tasklet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component("HelloTasklet1")
@StepScope
@Slf4j
public class HelloTasklet1 implements Tasklet {

	private static final String CSV_FILE_PATH = "src/main/resources/csv/backup";
	private static final String OK_DIRECTORY_PATH = "C:/pleiades-2024-06-java-win-64bit-jre_20240626/workspace/hello-spring-batch/src/main/resources/csv/output/ok/";
    private static final String NG_DIRECTORY_PATH = "C:/pleiades-2024-06-java-win-64bit-jre_20240626/workspace/hello-spring-batch/src/main/resources/csv/output/ng/";


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
    	log.info("CSV 파일을 읽고 OK와 NG 파일로 분류 중...");

        File csvDirectory = new File(CSV_FILE_PATH);
        if (!csvDirectory.exists() || !csvDirectory.isDirectory()) {
            log.error("CSV 파일 디렉토리가 존재하지 않습니다: {}", CSV_FILE_PATH);
            return RepeatStatus.FINISHED;
        }

        File[] csvFiles = csvDirectory.listFiles((dir, name) -> name.endsWith(".csv"));
        if (csvFiles == null || csvFiles.length == 0) {
            log.warn("CSV 파일이 없습니다.");
            return RepeatStatus.FINISHED;
        }

        Charset sjisCharset = Charset.forName("SJIS");

        for (File csvFile : csvFiles) {
            String baseFileName = csvFile.getName().replace(".csv", "");
            String okFilePath = OK_DIRECTORY_PATH + baseFileName + ".ok.csv";
            String ngFilePath = NG_DIRECTORY_PATH + baseFileName + ".ng.csv";
            boolean hasDataInOkFile = false;
            boolean hasDataInNgFile = false;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), sjisCharset));
                 BufferedWriter okWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(okFilePath), sjisCharset));
                 BufferedWriter ngWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ngFilePath), sjisCharset))) {

                log.info("처리 중: {}", csvFile.getName());

                // 첫 번째 줄 스킵
                br.readLine();

                String line;
                while ((line = br.readLine()) != null) {
                    String[] columns = line.split(",");
                    if (columns.length >= 3) {
                        String insuredNumber = columns[2];
                        if (insuredNumber.startsWith("A")) {
                            okWriter.write(line);
                            okWriter.newLine();
                            hasDataInOkFile = true;
                            log.info("OK 파일에 추가: {}", line);
                        } else if (insuredNumber.startsWith("B")) {
                            ngWriter.write(line);
                            ngWriter.newLine();
                            hasDataInNgFile = true;
                            log.info("NG 파일에 추가: {}", line);
                        }
                    } else {
                        log.warn("유효하지 않은 데이터 형식: {}", line);
                    }
                }

                // OK와 NG 파일의 .end 파일 생성
                createEndFile(OK_DIRECTORY_PATH + baseFileName + ".ok.end");
                createEndFile(NG_DIRECTORY_PATH + baseFileName + ".ng.end");

            } catch (IOException e) {
                log.error("CSV 파일 읽기 또는 쓰기 중 오류 발생: {}", csvFile.getName(), e);
            }

            // 빈 파일 삭제
            deleteIfEmpty(okFilePath, "OK");
            deleteIfEmpty(ngFilePath, "NG");
        }

        return RepeatStatus.FINISHED;
        
        
//        import com.itextpdf.text.Document;
//        import com.itextpdf.text.DocumentException;
//        import com.itextpdf.text.Element;
//        import com.itextpdf.text.Font;
//        import com.itextpdf.text.Paragraph;
//        import com.itextpdf.text.pdf.BaseFont;
//        import com.itextpdf.text.pdf.PdfContentByte;
//        import com.itextpdf.text.pdf.PdfPCell;
//        import com.itextpdf.text.pdf.PdfPTable;
//        import com.itextpdf.text.pdf.PdfWriter;
//
//        import java.io.FileOutputStream;
//        import java.io.IOException;
//
//        public class JapanesePDFExample {
//            public static void main(String[] args) {
//                Document document = new Document();
//                try {
//                    PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("output_sample_with_line_breaks.pdf"));
//                    document.open();
//
//                    // 일본어 폰트 로드 (폰트 파일 경로를 실제 폰트 위치로 변경해야 합니다)
//                    BaseFont baseFont = BaseFont.createFont("fonts/ipam.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
//                    Font font = new Font(baseFont, 8);  // Font size set to 8pt
//
//                    // 첫 번째 줄: 출력일자
//                    String outputDate = "出力日付：20241110";
//                    Paragraph dateParagraph = new Paragraph(outputDate, font);
//                    dateParagraph.setAlignment(Element.ALIGN_RIGHT);
//                    document.add(dateParagraph);
//
//                    // 두 번째 줄: 보헨자 번호 및 비보헨자 번호
//                    String insuranceInfo = "保険者番号：00442020  被保険者番号：0000000008";
//                    Paragraph insuranceParagraph = new Paragraph(insuranceInfo, font);
//                    insuranceParagraph.setAlignment(Element.ALIGN_RIGHT);
//                    document.add(insuranceParagraph);
//
//                    // 세 번째 줄: 좌측과 우측 내용
//                    PdfPTable table = new PdfPTable(2);
//                    table.setWidthPercentage(100);
//
//                    PdfPCell leftCell = new PdfPCell(new Paragraph("調査結果（特記事項）", font));
//                    leftCell.setBorder(PdfPCell.NO_BORDER);
//                    leftCell.setHorizontalAlignment(Element.ALIGN_LEFT);
//                    table.addCell(leftCell);
//
//                    PdfPCell rightCell = new PdfPCell(new Paragraph("調査実施日：20241110", font));
//                    rightCell.setBorder(PdfPCell.NO_BORDER);
//                    rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
//                    table.addCell(rightCell);
//
//                    document.add(table);
//
//                    // 네 번째 줄: １．身体機能・起居動作 (밑줄 포함)
//                    String fourthLine = "１．身体機能・起居動作";
//                    Paragraph fourthLineParagraph = new Paragraph(fourthLine, font);
//                    fourthLineParagraph.setAlignment(Element.ALIGN_LEFT);
//                    document.add(fourthLineParagraph);
//
//                    // 네 번째 줄 밑줄 그리기
//                    PdfContentByte cb = writer.getDirectContent();
//                    float x = document.leftMargin();
//                    float y = document.getVerticalPosition(true) - 2;
//                    float textWidth = baseFont.getWidthPoint(fourthLine, 8);
//
//                    cb.moveTo(x, y);
//                    cb.lineTo(x + textWidth, y);
//                    cb.stroke();
//
//                    // 긴 일본어 텍스트 줄바꿈 설정
//                    String longText = "えおかきくけこさしすせそたちつてと..."; // 실제 긴 텍스트로 대체
//                    int maxCharsPerLine = 60;
//
//                    // 각 섹션: １－１ to ３－１３ 추가
//                    for (int sectionGroup = 1; sectionGroup <= 3; sectionGroup++) {
//                        String sectionTitle = sectionGroup + "．身体機能・起居動作" + (sectionGroup == 1 ? "" : sectionGroup);
//                        Paragraph sectionParagraph = new Paragraph(sectionTitle, font);
//                        sectionParagraph.setAlignment(Element.ALIGN_LEFT);
//                        document.add(sectionParagraph);
//
//                        // 밑줄 추가
//                        y = document.getVerticalPosition(true) - 2;
//                        textWidth = baseFont.getWidthPoint(sectionTitle, 8);
//                        cb.moveTo(x, y);
//                        cb.lineTo(x + textWidth, y);
//                        cb.stroke();
//
//                        for (int section = 1; section <= 13; section++) {
//                            String subsectionTitle = sectionGroup + "－" + section + "．";
//                            Paragraph subsectionParagraph = new Paragraph(subsectionTitle, font);
//                            subsectionParagraph.setAlignment(Element.ALIGN_LEFT);
//                            document.add(subsectionParagraph);
//
//                            // 줄바꿈이 있는 텍스트 추가
//                            for (int i = 0; i < longText.length(); i += maxCharsPerLine) {
//                                String line = longText.substring(i, Math.min(i + maxCharsPerLine, longText.length()));
//                                Paragraph textParagraph = new Paragraph(line, font);
//                                textParagraph.setAlignment(Element.ALIGN_LEFT);
//                                document.add(textParagraph);
//                            }
//                        }
//                    }
//
//                    document.close();
//                } catch (DocumentException | IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }

        
        
    }

    private void createEndFile(String endFilePath) {
        File endFile = new File(endFilePath);
        try {
            if (endFile.createNewFile()) {
                log.info("완료 파일 생성: {}", endFilePath);
            }
        } catch (IOException e) {
            log.error("완료 파일 생성 중 오류 발생: {}", endFilePath, e);
        }
    }
    
    private void deleteIfEmpty(String filePath, String fileType) {
        File file = new File(filePath);
        if (file.exists() && file.length() == 0) {
            boolean deleted = file.delete();
            if (deleted) {
                log.info("빈 {} 파일을 삭제했습니다: {}", fileType, filePath);
            } else {
                log.warn("{} 파일 삭제 실패: {}", fileType, filePath);
            }
        } else {
            log.info("{} 파일이 존재하지 않거나 비어 있지 않습니다: {}", fileType, filePath);
        }
    }

}
