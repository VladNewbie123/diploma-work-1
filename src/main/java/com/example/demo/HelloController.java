package com.example.demo;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

public class HelloController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private TextArea Reports; // Поле для виведення звітів

    @FXML
    private TextArea Classification; // Поле для виведення класифікації файлів

    @FXML
    private TextArea metadata; // Поле для виведення метаданих файлів

    @FXML
    private TextArea welcomeText; // Поле для введення шляху до папки

    @FXML
    private Button copyButton; // Кнопка для запуску сортування

    @FXML
    void initialize() {
        // Прив'язуємо подію до кнопки: при натисканні викликається метод sortText
        copyButton.setOnAction(event -> sortText());
    }

    /**
     * Основний метод для обробки файлів у зазначеній папці.
     * Класифікує файли за типом, аналізує їх метадані та створює звіт.
     */
    private void sortText() {
        // Отримуємо шлях до папки з текстового поля
        String directoryPath = welcomeText.getText();
        if (directoryPath.isEmpty()) {
            Classification.setText("Поле введення порожнє. Введіть шлях до папки.");
            return;
        }

        try {
            Path folder = Paths.get(directoryPath);
            if (!Files.isDirectory(folder)) {
                Classification.setText("Вказаний шлях не є папкою.");
                return;
            }

            // Отримуємо список всіх файлів у папці та перетворюємо їх у об'єкти FileInfo
            List<FileInfo> files = Files.list(Paths.get(directoryPath))
                    .filter(Files::isRegularFile) // Залишаємо тільки файли
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        if (fileName.startsWith("~")) {
                            System.out.println("Тимчасовий файл виключено: " + fileName); // Логування тимчасових файлів
                            return false; // Виключаємо тимчасові файли
                        }
                        return true;
                    })
                    .filter(path -> !path.getFileName().toString().endsWith(".tmp")) // Виключаємо файли з розширенням .tmp
                    .map(this::createFileInfo) // Перетворюємо кожен шлях у об'єкт FileInfo
                    .collect(Collectors.toList());

            if (files.isEmpty()) {
                Classification.setText("Папка порожня або не містить відповідних файлів.");
            } else {
                // Групуємо файли за типом
                Map<String, List<FileInfo>> groupedFiles = files.stream()
                        .collect(Collectors.groupingBy(FileInfo::getType));

                // Обробляємо кожну групу файлів
                StringBuilder classificationResult = new StringBuilder();
                StringBuilder metadataInfo = new StringBuilder("Аналіз метаданих файлів:\n");

                groupedFiles.forEach((type, fileList) -> {
                    classificationResult.append(type).append(":\n");
                    fileList.forEach(file -> {
                        classificationResult.append("  ").append(file.getName()).append("\n");
                        metadataInfo.append(getFileMetadata(file.getPath()));
                    });
                    classificationResult.append("\n");
                });

                Classification.setText(classificationResult.toString());
                metadata.setText(metadataInfo.toString());
            }

            // Генерація звіту
            generateReport(folder);

        } catch (IOException e) {
            Classification.setText("Помилка читання папки. Перевірте шлях і спробуйте знову.");
        }
    }

    /**
     * Метод для створення звіту по файлах.
     */
    private void generateReport(Path folder) {
        try {
            int totalFiles = 0;
            int documentFiles = 0;
            int imageFiles = 0;
            int videoFiles = 0;

            // Фільтруємо файли: регулярні та не ті, що починаються з "~"
            for (Path file : Files.list(folder)
                    .filter(Files::isRegularFile)
                    .filter(f -> !f.getFileName().toString().startsWith("~")) // Виключаємо тимчасові файли
                    .collect(Collectors.toList())) {
                totalFiles++;
                String fileName = file.getFileName().toString().toLowerCase();
                if (fileName.endsWith(".docx") || fileName.endsWith(".pdf")) {
                    documentFiles++;
                } else if (fileName.endsWith(".jpg") || fileName.endsWith(".png")) {
                    imageFiles++;
                } else if (fileName.endsWith(".mp4") || fileName.endsWith(".avi")) {
                    videoFiles++;
                }
            }

            String report = String.format("""
                Загальний звіт:
                Усього файлів: %d
                Документів: %d
                Зображень: %d
                Відео: %d
                """, totalFiles, documentFiles, imageFiles, videoFiles);

            // Збереження звіту у файл
            try (PrintWriter writer = new PrintWriter("report.txt")) {
                writer.println(report);
            }

            // Виведення звіту у поле Reports
            Reports.setText(report);

        } catch (IOException e) {
            Reports.setText("Помилка під час створення звіту. Перевірте доступ до файлів.");
        }
    }

    /**
     * Метод для створення об'єкта FileInfo.
     */
    private FileInfo createFileInfo(Path path) {
        String fileName = path.getFileName().toString();
        String fileType = getFileExtension(fileName);
        return new FileInfo(fileName, fileType, path);
    }

    /**
     * Метод для отримання розширення файлу.
     */
    private String getFileExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(index + 1).toLowerCase() : "Інші файли";
    }

    /**
     * Метод для отримання метаданих файлу.
     */
    private String getFileMetadata(Path file) {
        try {
            long size = Files.size(file);
            var lastModified = Files.getLastModifiedTime(file);
            String mimeType = Files.probeContentType(file);

            return String.format("Файл: %s\n  Розмір: %d KB\n  Остання зміна: %s\n  Тип файлу: %s\n\n",
                    file.getFileName(), size / 1024, lastModified, mimeType != null ? mimeType : "Невідомий");
        } catch (IOException e) {
            return "Не вдалося отримати метадані для файлу: " + file.getFileName() + "\n";
        }
    }

    /**
     * Внутрішній клас для представлення інформації про файл.
     */
    private static class FileInfo {
        private final String name;
        private final String type;
        private final Path path;

        public FileInfo(String name, String type, Path path) {
            this.name = name;
            this.type = type;
            this.path = path;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public Path getPath() {
            return path;
        }
    }
}
