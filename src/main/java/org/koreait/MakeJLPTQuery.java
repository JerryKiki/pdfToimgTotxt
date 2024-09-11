package org.koreait;

import java.util.regex.*;
import java.util.*;

public class MakeJLPTQuery {

    public static void main(String[] args) {
        doMakeJLPTQuery("""
                    問題1 _ の言葉の読み方として最もよいものを、I 2 3 4から一つ選びなさい。(1*6)
                    1. あの態度には猛烈に腹が立った。

                    1 もれつ 2 きょうれつ 3 きょれつ 4 もうれつ

                    2.彼女は病を克服して、職場に戻ってきた。
                    | こうふく 2かくふく 3 かいふく 4 こくふく

                """);
    }

    //JLPT Query를 제작...
    public static void doMakeJLPTQuery(String selectedSentence) {

        //대표적인 OCR 오탈자 수정
        selectedSentence = selectedSentence.replace("|", "1");

        //questionBlock 나눔
        List<String[]> questionBlocks = makeQuestionBlocks(selectedSentence);

        //Block 별 문제 추출 및 분석
        List<String> parentQuestion = makeQueryOfParentQuestions(selectedSentence);
        List<String> questions = makeQueryOfQuestion(selectedSentence);
        List<String> options = findOption(selectedSentence);
        List<String> underlinedWords = findUnderlinedWord(selectedSentence);

        // 출력 확인
        System.out.println("ParentQuestion: " + parentQuestion);
        System.out.println("Questions: " + questions);
        System.out.println("Options: " + options);
        System.out.println("Underlined Words: " + underlinedWords);
    }

    //0. 감지된 텍스트를 가지고 상위문제를 기준으로 상위문제에 해당하는 하위문제끼리를 블록으로 만듦
    public static List<String[]> makeQuestionBlocks(String selectedSentence) {
        List<String[]> questionBlocks = new ArrayList<>();

        // "問題"로 시작하는 상위 문제와 그에 따른 하위 문제를 추출하는 정규 표현식
        Pattern pattern = Pattern.compile("(問題\\d+\\s.+?)(?=(問題\\d+|\\z))", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(selectedSentence);

        while (matcher.find()) {
            // 상위 문제와 그에 속한 하위 문제 묶음 (문제 텍스트를 블록 단위로 나눔)
            questionBlocks.add(new String[]{matcher.group(1)});
        }

        return questionBlocks;
    }

    // 1. 상위 문제(Parent Question) 추출해서 쿼리 작성
    public static List<String> makeQueryOfParentQuestions(String text) {

        //상위 문제관련 쿼리들을 순차적으로 집어넣을 List
        List<Map<String, String>> parentQuestionQueries = new ArrayList<>();

        //ParentQuestion + ParentQuestion_metadata를 저장할 Map
        Map<String, String> parentQuestionQuery = new HashMap<>();

        String qNum = "";
        String qText = "";

        // 패턴: "問題" 뒤에 숫자와 문제 텍스트 추출
        Pattern pattern = Pattern.compile("問題(\\d+)\\s(.+)");
        Matcher matcher = pattern.matcher(text);

        // 매칭되는 모든 상위 문제를 처리
        while (matcher.find()) {
            qNum = matcher.group(1);   // 문제 번호
            qText = matcher.group(2);  // 문제 텍스트

            // 쿼리 생성
            String parentQuestionQuery = "INSERT INTO parentQuestions SET regDate = NOW(), updateDate = NOW(),";
            parentQuestionQuery += " qNum = " + qNum + ",";
            parentQuestionQuery += " qText = '" + qText + "';";

            // 생성된 쿼리를 리스트에 추가
            parentQuestionQueries.add(parentQuestionQuery);
        }

        // 모든 쿼리 반환
        return parentQuestionQueries;
    }

    //상위문제의 메타데이터 쿼리 작성 - 상위문제 insert할때 추가된 ID 받아와서 generate하는 방식이 낫겠음
//    public static String makeQueryOfParentQuestion_metadata(String text) {
//        String parentQuestion_metadataQuery = "";
//
//        parentQuestion_metadataQuery = "INSERT INTO parentQuestion_metadata SET examYear = ?, examMonth = ?,";
//        parentQuestion_metadataQuery += "`level` = ?, category = ?";
//
//        return parentQuestion_metadataQuery;
//    }

    // 2. 하위 문제 추출
    public static List<String> makeQueryOfQuestion(String text) {
        List<String> questions = new ArrayList<>();

        //부모문제 번호
        String parentQNum = "";

        Pattern parentQNumPattern = Pattern.compile("問題(\\d+)");
        Matcher parentQNumMatcher = parentQNumPattern.matcher(text);
        while (parentQNumMatcher.find()) {
            parentQNum = parentQNumMatcher.group(1);
        }

        Pattern pattern = Pattern.compile("\\d+\\.");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            questions.add(matcher.group());
        }
        return questions;
    }

    // 3. 선택지 추출
    public static List<String> findOption(String text) {
        List<String> options = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\d\\s[^\n]+");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            options.add(matcher.group().trim());
        }
        return options;
    }

    // 4. 밑줄 단어 추출
    public static List<String> findUnderlinedWord(String text) {
        List<String> underlinedWords = new ArrayList<>();
        Pattern pattern = Pattern.compile("_ (\\S+)");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            underlinedWords.add(matcher.group(1));
        }
        return underlinedWords;
    }
}

//#########################
//import java.util.HashMap;
//import java.util.Map;
//
//public class MakeJLPTQuery {
//
//    public Object doMakeJLPTQuery(String selectedSentence) {
//        if (selectedSentence == null || selectedSentence.isEmpty()) {
//          return null;
//        }
//
//        if(selectedSentence.startsWith("問題")) {
//            Map<String, String> parentQuestionsQueries = makeQueryOfParentQuestions(selectedSentence);
//            return parentQuestionsQueries;
//        } else if(selectedSentence.startsWith()) {
//
//        }
//    }
//
//    public Map<String, String> makeQueryOfParentQuestions(String selectedSentence) {
//
//        Map<String, String> parentQuestionsQueries = new HashMap<String, String>();
//
//        //parentQuestion 쿼리
//        String qNum = selectedSentence.substring(2, 3); //問題 뒤의 숫자
//        String qText = selectedSentence.substring(3).trim(); //qNum 뒤의 문장.trim()
//
//        String parentQuestionQuery = "INSERT INTO parentQuestion SET regDate = NOW(), updateDate = NOW(),";
//        parentQuestionQuery += "qNum = " + qNum + ",";
//        parentQuestionQuery += "qText = " + qText + ";";
//
//        parentQuestionsQueries.put("parentQuestion", parentQuestionQuery);
//
//        //parentQuestion_metadata 쿼리
//        String parentQuestion_metadataQuery = "INSERT INTO parentQuestion_metadata SET examYear = ?, examMonth = ?,";
//        parentQuestion_metadataQuery += "`level` = ?, category = ?";
//
//        parentQuestionsQueries.put("parentQuestion_metadata", parentQuestion_metadataQuery);
//
//        return parentQuestionsQueries;
//    }
//}