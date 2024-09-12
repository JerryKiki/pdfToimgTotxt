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


    public int insertParentQuestion(String qNum, String qText, Map<String, Integer> metaDatas) {

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
            lastInsertedParentQuestionId = mapper.getLastInsertId();
            mapper.insertParentQuestion_metadata(lastInsertedParentQuestionId, examYear, examMonth, level, category);

            session.commit();  // 트랜잭션 커밋
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lastInsertedParentQuestionId;
    }

    public void insertQuestion(int parentQuestionId, Map<String, Integer> metaDatas, String qNum, String qText, String readingPassage, String option1, String option2, String option3, String option4) {
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
