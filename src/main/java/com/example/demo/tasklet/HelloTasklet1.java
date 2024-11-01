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
