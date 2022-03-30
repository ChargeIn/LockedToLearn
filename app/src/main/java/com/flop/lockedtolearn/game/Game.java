package com.flop.lockedtolearn.game;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.flop.lockedtolearn.R;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Game {

    static String vocabularyPath = "vocabulary.txt";
    static String statsPath = "stats.txt";
    static String v_separator = "<v-sep>";

    HashMap<String, String> all = new HashMap<>();
    String[] allKeys = new String[0];
    boolean isLoaded = false;
    boolean loading = false;
    boolean downloading = false;

    int setsCleared = 0;
    int setsFirstTry = 0;

    String[] learnSetKeys = new String[0];

    LottieAnimationView unlock_animation;
    TextView questionTextView;
    LinearLayout answerContainer;
    int solved = 0;
    static int end = 2;

    private final Context context;
    private final Vibrator vibrator;
    private final LayoutInflater inflater;

    private Runnable onSolveCallback;

    public Game(Context context, Vibrator vibrator) {
        this.context = context;
        this.vibrator = vibrator;
        this.inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void readVocabulary() {
        this.readStats();

        File path = context.getFilesDir();
        File vocabularyFile = new File(path, Game.vocabularyPath);

        if (!vocabularyFile.exists()) {
            return;
        }

        byte[] content = new byte[(int) vocabularyFile.length()];
        try {
            FileInputStream fileInputStream = new FileInputStream(vocabularyFile);
            fileInputStream.read(content);
            String strContent = new String(content);

            String[] lines = strContent.split("\n");

            for (String s : lines) {
                String[] line = s.split(Game.v_separator);
                this.all.put(line[0], line[1]);
            }
            this.allKeys = this.all.keySet().toArray(new String[0]);
            this.isLoaded = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readStats() {
        File path = context.getFilesDir();
        File statsFile = new File(path, Game.statsPath);
        if (!statsFile.exists()) {
            return;
        }

        byte[] content = new byte[(int) statsFile.length()];
        try {
            FileInputStream fileInputStream = new FileInputStream(statsFile);
            fileInputStream.read(content);
            String strContent = new String(content);

            String[] lines = strContent.split("\n");
            // 1. count of sets clear
            setsCleared = Integer.parseInt(lines[0]);
            // 2. count of sets cleared on first try
            setsFirstTry = Integer.parseInt(lines[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveAll() {
        StringBuilder strAll = new StringBuilder();

        for (Map.Entry<String, String> e : all.entrySet()) {
            strAll.append(e.getKey()).append(Game.v_separator).append(e.getValue()).append("\n");
        }

        File path = context.getFilesDir();

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(new File(path, Game.vocabularyPath));
            fileOutputStream.write(strAll.toString().getBytes());
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveStats() {
        StringBuilder stats = new StringBuilder();
        stats.append(this.setsCleared).append("\n");
        stats.append(this.setsFirstTry).append("\n");

        File path = context.getFilesDir();

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(new File(path, Game.statsPath));
            fileOutputStream.write(stats.toString().getBytes());
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String addFile(Uri fileUri) throws IOException {
        loading = true;
        if (!isLoaded) {
            this.readVocabulary();
        }

        // The temp file could be whatever you want
        InputStream inputStream = context.getContentResolver().openInputStream(fileUri);

        if (inputStream == null) {
            return "Failed to read input file.";
        }

        HSSFWorkbook workbook = new HSSFWorkbook(inputStream);
        HSSFSheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.iterator();

        int i = 0;
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

            // first 2 lines are headers info
            if (i++ < 2) {
                continue;
            }
            String key, value, annotation;

            Cell keyCell = row.getCell(0);
            if (keyCell == null) {
                continue;
            }
            key = keyCell.toString();

            //annotation = row.getCell(1).toString();

            Cell valueCell = row.getCell(2);
            if (valueCell == null) {
                continue;
            }
            value = valueCell.toString();

            // TODO: add annotation support
            this.all.put(key, value);
        }

        this.allKeys = this.all.keySet().toArray(new String[0]);

        this.saveAll();
        loading = false;
        return "Upload complete.";
    }

    public void prepareSet() {
        Log.i("---------------", "Preapare Set (is Loaded " + this.isLoaded);
        if (!this.isLoaded) {
            this.readVocabulary();
        }

        Set<String> learnSet = new HashSet<>();

        Log.i("---------------", "key Set " + this.allKeys.length);
        if (this.allKeys.length == 0) {
            return;
        }

        for (int i = 0; i < 40; i++) {
            int next = (int) (Math.random() * this.allKeys.length);
            learnSet.add(this.allKeys[next]);
        }
        this.learnSetKeys = learnSet.toArray(new String[0]);
    }

    public boolean isLoading() {
        return this.loading;
    }

    public boolean isDownloading() {
        return this.downloading;
    }

    public void setOnSolve(Runnable callback) {
        this.onSolveCallback = callback;
    }

    public View createLockView() {
        Log.i("----------------", "Create View");
        if (this.learnSetKeys.length == 0) {
            return null;
        }
        this.solved = 0;

        Log.i("----------------", "Was not empty");

        View view = this.inflater.inflate(R.layout.lock_screen, null);

        this.unlock_animation = view.findViewById(R.id.lottie_unlocked);
        this.questionTextView = view.findViewById(R.id.question_text);
        this.answerContainer = view.findViewById(R.id.answer_container);

        this.fillLockView();

        return view;
    }

    private void fillLockView() {
        int correctAnswer = (int) (Math.random() * this.learnSetKeys.length);
        this.questionTextView.setText(this.learnSetKeys[correctAnswer]);

        int[] wrongAnswers = new int[3];

        int i = 0;
        while (i < 3) {
            int nextWrong = (int) (Math.random() * this.learnSetKeys.length);

            if (nextWrong != correctAnswer && Arrays.stream(wrongAnswers).noneMatch(v -> nextWrong == v)) {
                wrongAnswers[i] = nextWrong;
                i++;
            }
        }

        LinearLayout answerLayoutBox = new LinearLayout(this.context);
        answerLayoutBox.setOrientation(LinearLayout.VERTICAL);

        int correctPos = (int) (Math.random() * 4);
        AtomicInteger tries = new AtomicInteger();

        i = 0;
        for (int j = 0; j < 4; j++) {
            View answer = this.inflater.inflate(R.layout.answer_button, null);
            Button btn = answer.findViewById(R.id.answer_button);

            if (j != correctPos) {
                btn.setOnClickListener((View view) -> {
                    this.vibrator.vibrate(VibrationEffect.createOneShot(100, 10));
                    btn.setTextColor(ContextCompat.getColor(this.context, R.color.failed_answer_btn_bg));
                    tries.getAndIncrement();
                });
                btn.setText(this.all.get(this.learnSetKeys[wrongAnswers[i]]));
                i++;
            } else {
                btn.setOnClickListener((View view) -> {
                    if (tries.get() == 0) {
                        this.setsFirstTry++;
                    }

                    if (this.solved > Game.end) {
                        this.setsCleared += this.solved + 1;
                        this.saveStats();
                        this.unlock_animation.setVisibility(View.VISIBLE);
                        this.unlock_animation.setSpeed(2);
                        this.unlock_animation.playAnimation();
                        Handler handler = new Handler(Looper.getMainLooper());
                        this.questionTextView.setVisibility(View.INVISIBLE);
                        this.answerContainer.setVisibility(View.INVISIBLE);
                        handler.postDelayed(() -> this.onSolveCallback.run(), 900);
                    } else {
                        this.answerContainer.removeView(answerLayoutBox);
                        this.fillLockView();
                    }
                    this.solved++;
                });
                btn.setText(this.all.get(this.learnSetKeys[correctAnswer]));
            }
            answerLayoutBox.addView(answer);
        }

        this.answerContainer.addView(answerLayoutBox);
    }

    public String writeTemplate(Uri uri) throws IOException {
        downloading = true;

        OutputStream fileOut = this.context.getContentResolver().openOutputStream(uri);

        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("Vocabulary");

        HSSFRow first = sheet.createRow(0);
        first.createCell(0).setCellValue("<Title>");

        sheet.createRow(1);

        HSSFRow header = sheet.createRow(2);
        header.createCell(0).setCellValue("<Native Language>");
        header.createCell(1).setCellValue("<Annotations>");
        header.createCell(2).setCellValue("<Translation>");

        sheet.createRow(3);
        sheet.createRow(4).createCell(0).setCellValue("<Start Here>");

        workbook.write(fileOut);
        fileOut.close();

        downloading = false;

        return "Download successful";
    }

    public void deleteAll() {
        this.all = new HashMap<>();
        this.allKeys = new String[0];
        this.learnSetKeys = new String[0];
        this.saveAll();
    }

    public String getStats() {
        if (!isLoaded) {
            readVocabulary();
        }

        this.readStats();

        double firstTryPercentage = (double) this.setsFirstTry / this.setsCleared;
        DecimalFormat df = new DecimalFormat("#.#");

        String stats = "";
        stats += "Vocabulary size: " + this.allKeys.length + "\n\n";
        stats += "Sets cleared: " + this.setsCleared + "\n\n";
        stats += "Sets cleared on first try: " + this.setsFirstTry + " (" + df.format(firstTryPercentage) + "%)\n\n";
        stats += "\n\nMore stats coming soon.";

        return stats;
    }
}
