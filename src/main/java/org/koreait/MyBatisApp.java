package org.koreait;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

public class MyBatisApp {
    private SqlSessionFactory sqlSessionFactory;

    // 생성자에서 MyBatis 초기화
    public MyBatisApp() {
        try {
            // MyBatis 설정 파일 로드
            Reader reader = Resources.getResourceAsReader("mybatis-config.xml");
            this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            this.sqlSessionFactory.getConfiguration().addMapper(MyBatisMapper.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Mapper에 ParentQuestion insert에 대한 정보를 최종적으로 전달
    public int insertParentQuestion(String qNum, String qText, Map<String, Integer> metaDatas) {

        //마지막으로 insert된 parentQuestion row의 id값을 받아올 변수
        int lastInsertedParentQuestionId = -1;

        try (SqlSession session = sqlSessionFactory.openSession()) {
            MyBatisMapper mapper = session.getMapper(MyBatisMapper.class);

            //metaDatas 압축풀기
            int examYear = -1;
            int examMonth = -1;
            int level = -1;
            int category = -1;

            if(metaDatas.containsKey("examYear")) {
                examYear = metaDatas.get("examYear");
            }
            if(metaDatas.containsKey("examMonth")) {
                examMonth = metaDatas.get("examMonth");
            }
            if(metaDatas.containsKey("level")) {
                level = metaDatas.get("level");
            }
            if(metaDatas.containsKey("category")) {
                category = metaDatas.get("category");
            }

            // 상위 문제 삽입
            mapper.insertParentQuestion(qNum, qText);
            // 삽입된 row의 id값 반환
            lastInsertedParentQuestionId = mapper.getLastInsertId();
            // id값을 가지고 metadata도 삽입
            mapper.insertParentQuestion_metadata(lastInsertedParentQuestionId, examYear, examMonth, level, category);

            session.commit();  // 트랜잭션 커밋 ==> 중요! 이거 안하면 쿼리 성공했어도 결과 반영 안됨.
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 삽입된 id값 반환
        return lastInsertedParentQuestionId;
    }

    //Mapper에 Question insert에 대한 정보를 최종적으로 전달
    //이건 id값을 딱히 이 함수를 실행한 메서드에서까지 반환받을 필요는 없어서 void로 작성함
    public void insertQuestion(int parentQuestionId, Map<String, Integer> metaDatas, String qNum, String qText, String readingPassage, String option1, String option2, String option3, String option4) {
        //마지막으로 insert된 parentQuestion row의 id값을 받아올 변수
        int lastInsertedQuestionId = -1;

        try (SqlSession session = sqlSessionFactory.openSession()) {
            MyBatisMapper mapper = session.getMapper(MyBatisMapper.class);

            //metaDatas 압축풀기
            int examYear = -1;
            int examMonth = -1;
            int level = -1;
            int category = -1;

            if (metaDatas.containsKey("examYear")) {
                examYear = metaDatas.get("examYear");
            }
            if (metaDatas.containsKey("examMonth")) {
                examMonth = metaDatas.get("examMonth");
            }
            if (metaDatas.containsKey("level")) {
                level = metaDatas.get("level");
            }
            if (metaDatas.containsKey("category")) {
                category = metaDatas.get("category");
            }

            if(readingPassage != null && (!readingPassage.isEmpty())) {
                mapper.insertQuestionWithReadingPassage(parentQuestionId, qNum, qText, readingPassage, option1, option2, option3, option4);
            } else {
                mapper.insertQuestionWithoutReadingPassage(parentQuestionId, qNum, qText, option1, option2, option3, option4);
            }

            //직전에 인서트된 아이디 불러오기
            lastInsertedQuestionId = mapper.getLastInsertId();
            //활용하여 메타데이터도 업데이트
            mapper.insertQuestion_metadata(lastInsertedQuestionId, examYear, examMonth, level, category);

            session.commit();  // 트랜잭션 커밋
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
