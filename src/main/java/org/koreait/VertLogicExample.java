package org.koreait;

import java.util.ArrayList;
import java.util.List;

public class VertLogicExample {
    public static void main(String[] args) {
        // 예시 텍스트
        List<String> originalLines = List.of(
                "朴 の 生 か と か り み し いと 人",
                "な カカ き も 確 さ み。 る た が は 間",
                "豊 強 る し か れ め と 下 そ < の",
                "か さ こ れ に まそ 系 の < 最",
                "さ と と ま 前 す 人 と 各 ま に も",
                "人 現住 ※ 近 を 間 き 地 た 住 松",
                "セ 代 ま 2 代 れ の に に をもちむ 活",
                "る の う し 的 ら 生 区 ぁあお和信 的",
                "の 私 こ か でて は 活 く〈 る れ 々 全",
                "で 寺 と し 非 現 の よ よら の 銘",
                "す の へ 人合 代 多 う 着 馬 生 求",
                "。 住 の 私 理 的 様 な の 着 寺 か",
                "環 欲 は な 価 な 表 住 う や ら",
                "境 求 そ も 値 在 現 ま ^ 気 生",
                "に の こ の 観 り の い 注 候 ま",
                "は 切 に に か 方 も を 風 れ",
                "な 実 人 見 ら に の 眺 も 圭 る",
                "い さ 々 え す 気 も め の の 住",
                "素 数 の る る づ あぁあ て で 達 い"
        );

        // Step 1: Remove all spaces
        List<String> cleanedLines = new ArrayList<>();
        for (String line : originalLines) {
            cleanedLines.add(line.replaceAll("\\s+", ""));
        }

        // Step 2: Create a list to store reversed rows
        List<String> newRows = new ArrayList<>();
        List<String> oldRows = new ArrayList<>(cleanedLines);

        while (!oldRows.isEmpty()) {
            // Process each row
            List<String> tempRow = new ArrayList<>(oldRows);
            oldRows.clear();

            // Step 3: Build new rows from the end to the start
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

            // Remove processed rows
            oldRows = tempRow.stream()
                    .filter(row -> row.length() > maxIndex) // Keep only rows longer than the processed index
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }

        // Step 4: Combine new rows into one text
        String finalText = String.join("\n", newRows);

        // First println after applying custom logic
        System.out.println("After custom logic:");
        System.out.println(finalText);
    }
}



