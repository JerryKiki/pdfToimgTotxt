package org.koreait;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MyBatisMapper {

    @Insert("""
    INSERT INTO ParentQuestion
    SET regDate = NOW(),
    updateDate = NOW(),
    qNum = #{qNum},
    qText = #{qText}
    """)
    public void insertParentQuestion(@Param("qNum")String qNum, @Param("qText")String qText);

    @Select("SELECT LAST_INSERT_ID();")
    public int getLastInsertId();

    @Insert("""
    INSERT INTO ParentQuestion_metadata
    SET parentQuestionId = #{lastInsertedParentQuestionId},
    examYear = #{examYear},
    examMonth = #{examMonth},
    `level` = #{level},
    category = #{category}
    """)
    void insertParentQuestion_metadata(@Param("lastInsertedParentQuestionId")int lastInsertedParentQuestionId, @Param("examYear")int examYear, @Param("examMonth")int examMonth, @Param("level")int level, @Param("category")int category);

    @Insert("""
    INSERT INTO Question
    SET regDate = NOW(),
    updateDate = NOW(),
    parentQuestionId = #{parentQuestionId},
    qNum = #{qNum},
    qText = #{qText},
    readingPassage = #{readingPassage},
    option1 = #{option1},
    option2 = #{option2},
    option3 = #{option3},
    option4 = #{option4}
    """)
    void insertQuestionWithReadingPassage(@Param("parentQuestionId")int parentQuestionId, @Param("qNum")String qNum, @Param("qText")String qText, @Param("readingPassage")String readingPassage, @Param("option1")String option1, @Param("option2")String option2, @Param("option3")String option3, @Param("option4")String option4);

    @Insert("""
    INSERT INTO Question
    SET regDate = NOW(),
    updateDate = NOW(),
    parentQuestionId = #{parentQuestionId},
    qNum = #{qNum},
    qText = #{qText},
    option1 = #{option1},
    option2 = #{option2},
    option3 = #{option3},
    option4 = #{option4}
    """)
    void insertQuestionWithoutReadingPassage(@Param("parentQuestionId")int parentQuestionId, @Param("qNum")String qNum, @Param("qText")String qText, @Param("option1")String option1, @Param("option2")String option2, @Param("option3")String option3, @Param("option4")String option4);

    @Insert("""
    INSERT INTO Question_metadata
    SET QuestionId = #{lastInsertedQuestionId},
    examYear = #{examYear},
    examMonth = #{examMonth},
    `level` = #{level},
    category = #{category}
    """)
    void insertQuestion_metadata(@Param("lastInsertedQuestionId")int lastInsertedQuestionId, @Param("examYear")int examYear, @Param("examMonth")int examMonth, @Param("level")int level, @Param("category")int category);
}
