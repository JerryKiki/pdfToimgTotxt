package org.koreait;

import java.util.regex.*;
import java.util.*;

public class MakeJLPTQuery {

    public static void main(String[] args) {
        doMakeJLPTQuery("""
                    examYear = 2019,
                    examMonth = 7,
                    level = 1,
                    category = 1

                    問題1 _ の言葉の読み方として最もよいものを、I 2 3 4から一つ選びなさい。(|*6)
                    1. あの態度には和猛烈に腹が立った。
                    本文がある状況のテストのためのダミーデータです。本文がある状況のテストのためのダミーデータです。
                    本文がある状況のテストのためのダミーデータです。本文がある状況のテストのためのダミーデータです。本文がある状況のテストのためのダミーデータです。
                    1 もれつ 2 きょうれつ 3 きょれつ 4 もうれつ
                    
                    2.彼女は病を克服して、職場に戻ってきた。
                    | こうふく 2かくふく 3 かいふく 4 こくふく
                    
                    3.前れやすいので、運ぶときは気をつけてください。
                    1 つぶれやすい 2 くずれやすい
                    3 はがれやすい 4 こぼれやすい
                    
                    4.この薬にはウイルスの繁殖を折える効果がある。
                    | はんしょく 2 はんちょく 3 ばんしょく 4。ばんあまく
                    
                    5.サイトの履歴は残っていなかった。
                    1 ふくれき 2 られき 3 りれき 。刈ふ信き
                    
                    6.夕日に赤く映える山を写真に収めた。
                    1 そびえる 2 はえる 3 もをる*、4 さえる
                    
                    問題2 (    ) に入れるのに最もよいものを、| 2 3 4から一つ選びなさい。(1*7)
                    7.がガスが漏れると、(   )が感知して乏報が鳴る。
                    1 レーダー 2 モーター 3 センサー 4 レバー
                    
                    8.彼はロケットを作って宇宙へ飛ばしたいという(    )夢を実現した。
                    1 絶大な 2盛大な 3 雑大な 4壮大な
                    
                    9.その優しいメロディーは私の耳に(   )敗き、眠りに謗ってくれた。
                    1 ここちよく 2 いさぎよく 3 喜ばしく 4 輝かしく
                    
                    10.兄は科学者として遺伝子の研究に(    )している。
                    1 従事 2勤務 3在籍 4就労
                    
                    11. 葉書が届いたが、雨でインクが少し(     )、読みにくかった。
                    | 暴れて 2 にじんで 3 震えて 4 ゆがんで
                """);
    }

    //분석해서 쿼리 삽입(이 클래스의 main같은 함수)
    public static void doMakeJLPTQuery(String selectedSentence) {

        //대표적인 OCR 오탈자 수정
        selectedSentence = selectedSentence.replace("|", "1");
        selectedSentence = selectedSentence.replace("I", "1");

        //텍스트 정리
        selectedSentence = selectedSentence.replaceAll("\\t", "")        // 탭 제거
                                            .replaceAll(" +", " ")        // 여러 개의 공백을 하나의 공백으로
                                            .replaceAll("[\\n\\r]+", "\n"); // 여러 개의 줄바꿈을 하나의 줄바꿈으로

        //questionBlock 나눔
        List<String[]> parentQuestionBlocks = makeParentQuestionBlocks(selectedSentence);
        ;
        Map<String, Integer> metaDatas = getMetaDatas(selectedSentence);

        //Update시 검수를 위한 count
        int insertedParentQuestionCount = 0;
        int insertedQuestionCount = 0;

        //Block 별 문제 쿼리 삽입
        for (String[] parentQuestionBlock : parentQuestionBlocks) {
            //부모문제 삽입 후 Id 받아오기 (metaData는 여기서 함께 수행한다)
            int parentQuestionId = insertParentQuestion(parentQuestionBlock[0], metaDatas);
            int insertedQuestionCountPerBlock = insertQuestions(parentQuestionBlock[0], metaDatas, parentQuestionId);

            insertedParentQuestionCount++;
            insertedQuestionCount += insertedQuestionCountPerBlock;
        }

        // 출력 확인
        System.out.println("Inserted ParentQuestions Count: " + insertedParentQuestionCount);
        System.out.println("Inserted Questions Count: " + insertedQuestionCount);
    }

    //0. 감지된 텍스트를 가지고 상위문제를 기준으로 상위문제에 해당하는 하위문제끼리를 블록으로 만듦
    public static List<String[]> makeParentQuestionBlocks(String selectedSentence) {
        List<String[]> parentQuestionBlocks = new ArrayList<>();

        // "問題"로 시작하는 상위 문제와 그에 따른 하위 문제를 추출하는 정규 표현식
        Pattern pattern = Pattern.compile("(問題\\d+\\s.+?)(?=(問題\\d+|\\z))", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(selectedSentence);

        while (matcher.find()) {
            // 상위 문제와 그에 속한 하위 문제 묶음 (문제 텍스트를 블록 단위로 나눔)
            parentQuestionBlocks.add(new String[]{matcher.group(1)});
        }

        return parentQuestionBlocks;
    }

    //0. 감지된 텍스트에서 metadata를 추출하는 함수
    public static Map<String, Integer> getMetaDatas(String selectedSentence) {
        Map<String, Integer> metaDatas = new HashMap<>();

        // 정규 표현식
        String regex = "examYear\\s*=\\s*(\\d{4})\\s*,\\s*examMonth\\s*=\\s*(\\d{1,2})\\s*,\\s*level\\s*=\\s*([1-5])\\s*,\\s*category\\s*=\\s*([1-3])";

        // 패턴과 매처 생성
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(selectedSentence);

        // 값 저장을 위한 변수들
        int examYear = -1;
        int examMonth = -1;
        int level = -1;
        int category = -1;

        // 정규 표현식 매칭
        if (matcher.find()) {
            examYear = Integer.parseInt(matcher.group(1));  // 4자리 숫자
            examMonth = Integer.parseInt(matcher.group(2)); // 1~2자리 숫자
            level = Integer.parseInt(matcher.group(3));     // 1~5 사이의 정수
            category = Integer.parseInt(matcher.group(4));  // 1~3 사이의 정수

            metaDatas.put("examYear", examYear);
            metaDatas.put("examMonth", examMonth);
            metaDatas.put("level", level);
            metaDatas.put("category", category);
        }

        return metaDatas;
    }

    // 1. 상위 문제(Parent Question) 추출해서 Insert
    public static int insertParentQuestion(String text, Map<String, Integer> metaDatas) {

        int lastInsertedParentQuestionId = 0;

        String qNum = "";
        String qText = "";

        // 패턴: "問題" 뒤에 숫자와 문제 텍스트 추출
        Pattern pattern = Pattern.compile("問題(\\d+)\\s(.+)");
        Matcher matcher = pattern.matcher(text);

        // 매칭되는 상위 문제를 처리
        while (matcher.find()) {
            qNum = matcher.group(1);   // 문제 번호
            qText = matcher.group(2);  // 문제 텍스트

            // 인서트
            MyBatisApp myBatisApp = new MyBatisApp();
            lastInsertedParentQuestionId = myBatisApp.insertParentQuestion(qNum, qText, metaDatas);
        }

        //부모 질문 id 반환
        return lastInsertedParentQuestionId;
    }

    //2-1. 하위 문제들을 블럭화 (인자로 이미 블럭화된 묶음을 받음)
    public static List<String[]> makeQuestionBlocks(String blockedQuestions) {
        List<String[]> QuestionBlocks = new ArrayList<>();

        // 하위 문제와 선택지를 블럭 단위로 추출하는 정규 표현식
        Pattern pattern = Pattern.compile("(\\d+\\..+?)(?=\\d+\\.|\\z)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(blockedQuestions);

        while (matcher.find()) {
            // 하위 문제와 선택지를 블록 단위로 나눔
            QuestionBlocks.add(new String[]{matcher.group(1)});
        }

        return QuestionBlocks;
    }

    // 2-2. 각각의 하위 문제를 Insert
    public static int insertQuestions(String text, Map<String, Integer> metaDatas, int parentQuestionId) {

        int insertedQuestionCount = 0;
        List<String[]> QuestionBlocks = makeQuestionBlocks(text);

        for (String[] questionBlock : QuestionBlocks) {

            //필요한 변수 선언
            String qNum = "";
            String qText = "";
            String readingPassage = "";
            String option1 = "";
            String option2 = "";
            String option3 = "";
            String option4 = "";

            //qNum + qText 패턴&매쳐
            Pattern qNumAndqTextPattern = Pattern.compile("(\\d+)\\.\\s*(.+)\\n");
            Matcher qNumAndqTextMatcher = qNumAndqTextPattern.matcher(questionBlock[0]);
            if (qNumAndqTextMatcher.find()) {
                qNum = qNumAndqTextMatcher.group(1);
                qText = qNumAndqTextMatcher.group(2);
            }

            //readingPassage 패턴&매쳐
            Pattern readingPassagePattern = Pattern.compile("\\n(.*?)\\n1\\s", Pattern.DOTALL);
            Matcher readingPassageMatcher = readingPassagePattern.matcher(questionBlock[0]);
            if (readingPassageMatcher.find()) {
                readingPassage = readingPassageMatcher.group(1);
            }

            //option1~4 패턴&매쳐
            Pattern option1Pattern = Pattern.compile("1\\s(.+?)(?=\\s2\\s)");
            Matcher option1Matcher = option1Pattern.matcher(questionBlock[0]);
            if (option1Matcher.find()) {
                option1 = option1Matcher.group(1);
            }

            Pattern option2Pattern = Pattern.compile("2\\s(.+?)(?=\\s3\\s)");
            Matcher option2Matcher = option2Pattern.matcher(questionBlock[0]);
            if (option2Matcher.find()) {
                option2 = option2Matcher.group(1);
            }

            Pattern option3Pattern = Pattern.compile("3\\s(.+?)(?=\\s4\\s)");
            Matcher option3Matcher = option3Pattern.matcher(questionBlock[0]);
            if (option3Matcher.find()) {
                option3 = option3Matcher.group(1);
            }

            Pattern option4Pattern = Pattern.compile("4\\s(.+)");
            Matcher option4Matcher = option4Pattern.matcher(questionBlock[0]);
            if (option4Matcher.find()) {
                option4 = option4Matcher.group(1);
            }

            //인서트
            MyBatisApp myBatisApp = new MyBatisApp();
            myBatisApp.insertQuestion(parentQuestionId, metaDatas, qNum, qText, readingPassage, option1, option2, option3, option4);

            insertedQuestionCount++;

        }

        return insertedQuestionCount;
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