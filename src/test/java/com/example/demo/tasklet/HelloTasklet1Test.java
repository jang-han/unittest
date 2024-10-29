package com.example.demo.tasklet;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

public class HelloTasklet1Test {

    @InjectMocks
    private HelloTasklet1 tasklet;

    @Mock
    private StepContribution contribution;

    @Mock
    private ChunkContext chunkContext;

    private AutoCloseable closeable;

    private static final String BASE_PATH = "src/main/resources/csv/output";
    private static final String OK_FILE_PATH = BASE_PATH + "/ok/20241029.ok.csv";
    private static final String NG_FILE_PATH = BASE_PATH + "/ng/20241029.ng.csv";
    private static final String INPUT_FILE_PATH = "src/main/resources/csv/backup/20241029.csv";

    @BeforeEach
    public void setUp() throws IOException {
        closeable = MockitoAnnotations.openMocks(this);
        createSampleCsvFile(INPUT_FILE_PATH);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
        deleteFile(INPUT_FILE_PATH);
        deleteFile(OK_FILE_PATH);
        deleteFile(NG_FILE_PATH);
        deleteFile(OK_FILE_PATH.replace(".csv", ".end"));
        deleteFile(NG_FILE_PATH.replace(".csv", ".end"));
    }

    @Test
    public void testExecute() throws Exception {
        // Act
        RepeatStatus status = tasklet.execute(contribution, chunkContext);

        // Assert
        assertEquals(RepeatStatus.FINISHED, status);
        //File okFile = new File("C:/pleiades-2024-06-java-win-64bit-jre_20240626/workspace/hello-spring-batch/src/main/resources/csv/output/ok/20241029.ok.csv");
        assertTrue(new File(OK_FILE_PATH).exists(), "OK 파일이 생성되지 않았습니다.");
        assertTrue(new File(NG_FILE_PATH).exists(), "NG 파일이 생성되지 않았습니다.");
        assertTrue(new File(OK_FILE_PATH.replace(".csv", ".end")).exists(), "OK end 파일이 생성되지 않았습니다.");
        assertTrue(new File(NG_FILE_PATH.replace(".csv", ".end")).exists(), "NG end 파일이 생성되지 않았습니다.");

        // OK, NG 파일의 내용이 올바른지 확인
        assertTrue(Files.readAllLines(Paths.get(OK_FILE_PATH), Charset.forName("SJIS")).size() > 0, "OK 파일에 데이터가 없습니다.");
        assertTrue(Files.readAllLines(Paths.get(NG_FILE_PATH), Charset.forName("SJIS")).size() > 0, "NG 파일에 데이터가 없습니다.");
    }

    private void createSampleCsvFile(String path) throws IOException {
        File file = new File(path);
        file.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, Charset.forName("SJIS")))) {
            writer.write("Header\n"); // 첫 번째 줄을 헤더로 사용하여 스킵
            writer.write("Sample,Data1,A\n");
            writer.write("Sample,Data2,B\n");
        }
    }

    private void deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }
}
