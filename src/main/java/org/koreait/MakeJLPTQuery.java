package org.koreait;

import java.util.regex.*;
import java.util.*;

public class MakeJLPTQuery {

//    public static void main(String[] args) {
//        doMakeJLPTQuery("""
//                    examYear = 2019,
//                    examMonth = 7,
//                    level = 1,
//                    category = 1
//
//                    問題1 _ の言葉の読み方として最もよいものを、I 2 3 4から一つ選びなさい。(|*6)
//                    1. あの態度には和猛烈に腹が立った。
//                    本文がある状況のテストのためのダミーデータです。本文がある状況のテストのためのダミーデータです。
//                    本文がある状況のテストのためのダミーデータです。本文がある状況のテストのためのダミーデータです。本文がある状況のテストのためのダミーデータです。
//                    1 もれつ 2 きょうれつ 3 きょれつ 4 もうれつ
//
//                    2.彼女は病を克服して、職場に戻ってきた。
//                    | こうふく 2かくふく 3 かいふく 4 こくふく
//
//                    3.前れやすいので、運ぶときは気をつけてください。
//                    1 つぶれやすい 2 くずれやすい
//                    3 はがれやすい 4 こぼれやすい
//
//                    4.この薬にはウイルスの繁殖を折える効果がある。
//                    | はんしょく 2 はんちょく 3 ばんしょく 4 ばんあまく
//
//                    5.サイトの履歴は残っていなかった。
//                    1 ふくれき 2 られき 3 りれき 4 刈ふ信き
//
//                    6.夕日に赤く映える山を写真に収めた。
//                    1 そびえる 2 はえる 3 もをる 4 さえる
//
//                    問題2 (    ) に入れるのに最もよいものを、| 2 3 4から一つ選びなさい。(1*7)
//                    7.がガスが漏れると、(   )が感知して乏報が鳴る。
//                    1 レーダー 2 モーター 3 センサー 4 レバー
//
//                    8.彼はロケットを作って宇宙へ飛ばしたいという(    )夢を実現した。
//                    1 絶大な 2盛大な 3 雑大な 4壮大な
//
//                    9.その優しいメロディーは私の耳に(   )敗き、眠りに謗ってくれた。
//                    1 ここちよく 2 いさぎよく 3 喜ばしく 4 輝かしく
//
//                    10.兄は科学者として遺伝子の研究に(    )している。
//                    1 従事 2勤務 3在籍 4就労
//
//                    11. 葉書が届いたが、雨でインクが少し(     )、読みにくかった。
//                    | 暴れて 2 にじんで 3 震えて 4 ゆがんで
//                """);
//    }

    //만들어둔 각종 텍스트 분석 및 쿼리삽입용 함수들을 순서에 맞게 실행해주는 함수
    public static void doMakeJLPTQuery(String selectedSentence) {

        //대표적인 OCR 오탈자 수정
        selectedSentence = selectedSentence.replace("|", "1");
        selectedSentence = selectedSentence.replace("I", "1");

        //정규표현식을 위한 옵션 포맷 정리
        selectedSentence = selectedSentence.replaceAll("(\\d+)([^.\\s\\d])", "$1 $2");

        //텍스트 정리
        selectedSentence = selectedSentence.replaceAll("\\t", "")        // 탭 제거
                                            .replaceAll(" +", " ")        // 여러 개의 공백을 하나의 공백으로
                                            .replaceAll("[\\n\\r]+", "\n"); // 여러 개의 줄바꿈을 하나의 줄바꿈으로

        //parentQuestion을 기준으로 블록을 나누는 함수 실행
        List<String[]> parentQuestionBlocks = makeParentQuestionBlocks(selectedSentence);
        //텍스트에 명기된 metaData를 저장하는 함수 실행
        Map<String, Integer> metaDatas = getMetaDatas(selectedSentence);

        //이번 실행 시 DB에 삽입된 데이터 개수를 세어주는 변수 (추후 리턴값으로 바꿔 검수 시 사용예정)
        int insertedParentQuestionCount = 0;
        int insertedQuestionCount = 0;

        //생성된 부모문제 Block별로 나누어 문제 insert를 실행
        for (String[] parentQuestionBlock : parentQuestionBlocks) {
            //부모문제 + 메타데이터 삽입 후 직전에 삽입된 Id 받아오기
            int parentQuestionId = insertParentQuestion(parentQuestionBlock[0], metaDatas);
            //방금 삽입된 부모문제의 하위문제들 + 메타데이터를 삽입하고 삽입된 개수를 받아옴 (블럭별 개수가 저장됨)
            int insertedQuestionCountPerBlock = insertQuestions(parentQuestionBlock[0], metaDatas, parentQuestionId);

            //부모문제 삽입개수++;
            insertedParentQuestionCount++;
            //하위문제 삽입개수 더하기
            insertedQuestionCount += insertedQuestionCountPerBlock;
        }

        // 출력으로 개수 확인
        System.out.println("Inserted ParentQuestions Count: " + insertedParentQuestionCount);
        System.out.println("Inserted Questions Count: " + insertedQuestionCount);
    }

    //0. 감지된 텍스트를 가지고 상위문제를 기준으로 상위문제 + 해당하는 하위문제들을 하나의 블록으로 만듦
    public static List<String[]> makeParentQuestionBlocks(String selectedSentence) {

        // 블록들을 저장할 리스트
        List<String[]> parentQuestionBlocks = new ArrayList<>();

        // "問題"로 시작하는 상위 문제와 그에 따른 하위 문제를 추출하는 정규 표현식
        Pattern pattern = Pattern.compile("(問題\\d+\\s.+?)(?=(問題\\d+|\\z))", Pattern.DOTALL);
        // 인자값으로 받은 텍스트를 대상으로 정규표현식에 매치되는 부분을 찾아줌
        Matcher matcher = pattern.matcher(selectedSentence);

        // 실제 매칭을 수행
        while (matcher.find()) { // 매칭되는 것을 찾으면
            // 상위 문제와 그에 속한 하위 문제 묶음을 블록 단위로 리스트에 저장
            parentQuestionBlocks.add(new String[]{matcher.group(1)});
        }

        // 리스트 리턴
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

    // 1. 상위 문제(Parent Question) 추출해서 insert
    public static int insertParentQuestion(String text, Map<String, Integer> metaDatas) {

        //metadata에 보내줄 직전 삽입된 parentQuestionId를 담을 변수
        int lastInsertedParentQuestionId = 0;

        //필요한 변수 선언
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
            MyBatisApp myBatisApp = new MyBatisApp(); //인서트를 수행할 MyBatisApp 객체 생성
            lastInsertedParentQuestionId = myBatisApp.insertParentQuestion(qNum, qText, metaDatas); //인서트 후 인서트된 Id 리턴받음
        }

        //부모 질문의 id 반환(하위의 Question을 insert할때 필요한 정보이기 때문임)
        return lastInsertedParentQuestionId;
    }

    // 2-1. 블럭처리된 부모 문제의 하위 문제들을 Insert
    public static int insertQuestions(String text, Map<String, Integer> metaDatas, int parentQuestionId) {

        //인서트된 하위문제의 개수를 세어주는 변수
        int insertedQuestionCount = 0;
        //문제들을 각각의 문제 하나하나로 블럭화해주는 함수 실행
        List<String[]> QuestionBlocks = makeQuestionBlocks(text);

        //모든 블럭에 대하여 반복 수행
        for (String[] questionBlock : QuestionBlocks) {

            //필요한 변수 선언
            String qNum = ""; //문제 번호(!= db상의 id, 기출문제 상으로 시에 이 문제가 몇번이었는지를 저장함)
            String qText = ""; //문제 텍스트
            String readingPassage = ""; //독해 문제 등, 읽어야하는 단락이 있을 때 필요한 변수
            String option1 = ""; //보기1번
            String option2 = ""; //보기2번
            String option3 = ""; //보기3번
            String option4 = ""; //보기4번

            //qNum + qText 패턴&매쳐
            Pattern qNumAndqTextPattern = Pattern.compile("(\\d+)\\.\\s*(.+)\\n");
            Matcher qNumAndqTextMatcher = qNumAndqTextPattern.matcher(questionBlock[0]);
            if (qNumAndqTextMatcher.find()) {
                qNum = qNumAndqTextMatcher.group(1);
                qText = qNumAndqTextMatcher.group(2);
            }

            //readingPassage 패턴&매쳐
            Pattern readingPassagePattern = Pattern.compile("\\n(.*?)\\n(?=\\s*1\\s)", Pattern.DOTALL);
            Matcher readingPassageMatcher = readingPassagePattern.matcher(questionBlock[0]);
            if (readingPassageMatcher.find()) {
                readingPassage = readingPassageMatcher.group(1);
            }

            //option1~4 패턴&매쳐
            Pattern option1Pattern = Pattern.compile("1\\s+(.+?)(?=\\s+2\\s+|\\s+3\\s+|\\s+4\\s+|\\z|\\n)", Pattern.DOTALL);
            Matcher option1Matcher = option1Pattern.matcher(questionBlock[0]);
            if (option1Matcher.find()) {
                option1 = option1Matcher.group(1);
            }

            Pattern option2Pattern = Pattern.compile("2\\s+(.+?)(?=\\s+3\\s+|\\s+4\\s+|\\z|\\n)", Pattern.DOTALL);
            Matcher option2Matcher = option2Pattern.matcher(questionBlock[0]);
            if (option2Matcher.find()) {
                option2 = option2Matcher.group(1);
            }

            Pattern option3Pattern = Pattern.compile("3\\s+(.+?)(?=\\s+4\\s+|\\z|\\n)", Pattern.DOTALL);
            Matcher option3Matcher = option3Pattern.matcher(questionBlock[0]);
            if (option3Matcher.find()) {
                option3 = option3Matcher.group(1);
            }

            Pattern option4Pattern = Pattern.compile("4\\s+(.+?)(?=\\n|$)", Pattern.DOTALL);
            Matcher option4Matcher = option4Pattern.matcher(questionBlock[0]);
            if (option4Matcher.find()) {
                option4 = option4Matcher.group(1);
            }

            //인서트
            MyBatisApp myBatisApp = new MyBatisApp();
            myBatisApp.insertQuestion(parentQuestionId, metaDatas, qNum, qText, readingPassage, option1, option2, option3, option4);

            //인서트 할 때마다 인서트된 문제 수를 ++
            insertedQuestionCount++;

        }

        //인서트된 문제 수를 반환
        return insertedQuestionCount;
    }

    //2-2. 하위 문제들도 문제 별로 블럭화 (인자로 이미 부모문제 기준으로 블럭화된 묶음을 받음 ==> 문제 별로 작은 블럭을 생성한다고 보면 됨)
    public static List<String[]> makeQuestionBlocks(String blockedQuestions) {
        //블럭을 담을 리스트
        List<String[]> QuestionBlocks = new ArrayList<>();

        // 하위 문제와 선택지를 블럭 단위로 추출하는 정규 표현식
        Pattern pattern = Pattern.compile("(\\d+\\..+?)(?=\\d+\\.|\\z)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(blockedQuestions);

        while (matcher.find()) {
            // 하위 문제와 선택지를 블록 단위로 나누고 리스트에 추가
            QuestionBlocks.add(new String[]{matcher.group(1)});
        }

        //리스트 반환
        return QuestionBlocks;
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