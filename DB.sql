DROP DATABASE test2;
CREATE DATABASE test2;
USE test2;

CREATE TABLE `ParentQuestion`(
                                 parentQuestionId INT(10) UNSIGNED PRIMARY KEY AUTO_INCREMENT NOT NULL,
                                 regDate DATETIME NOT NULL,
                                 updateDate DATETIME NOT NULL,
                                 qNum INT(10) NOT NULL,
                                 qText TEXT NOT NULL
);

CREATE TABLE `ParentQuestion_metadata`(
                                          parentQuestionId INT(10) UNSIGNED PRIMARY KEY NOT NULL,
                                          examYear INT(10) NOT NULL,
                                          examMonth INT(10) NOT NULL,
                                          `level` INT(10) NOT NULL,
                                          category INT(10) NOT NULL,
                                          FOREIGN KEY (parentQuestionId) REFERENCES ParentQuestion(parentQuestionId) ON DELETE CASCADE
);

CREATE TABLE `Question`(
                           questionId INT(10) UNSIGNED PRIMARY KEY AUTO_INCREMENT NOT NULL,
                           regDate DATETIME NOT NULL,
                           updateDate DATETIME NOT NULL,
                           parentQuestionId INT(10) UNSIGNED,
                           qNum INT(10) NOT NULL,
                           qText TEXT,
                           `text` TEXT NOT NULL,
                           option1 TEXT NOT NULL,
                           option2 TEXT NOT NULL,
                           option3 TEXT NOT NULL,
                           option4 TEXT NOT NULL,
                           corAnswer INT(10) NOT NULL,
                           FOREIGN KEY (parentQuestionId) REFERENCES ParentQuestion(parentQuestionId) ON DELETE SET NULL
);

CREATE TABLE `Question_metadata`(
                                    questionId INT(10) UNSIGNED PRIMARY KEY NOT NULL,
                                    examYear INT(10) NOT NULL,
                                    examMonth INT(10) NOT NULL,
                                    `level` INT(10) NOT NULL,
                                    category INT(10) NOT NULL,
                                    solvedUsrCount INT(10) DEFAULT 0,
                                    correctedUsrCount INT(10) DEFAULT 0,
                                    explanation TEXT,
                                    FOREIGN KEY (questionId) REFERENCES Question(questionId) ON DELETE CASCADE
);

CREATE TABLE `ListeningFile`(
                                fileId INT(10) UNSIGNED PRIMARY KEY AUTO_INCREMENT NOT NULL,
                                questionId INT(10) UNSIGNED,
                                regDate DATETIME NOT NULL,
                                updateDate DATETIME NOT NULL,
                                fileUrl VARCHAR(255) NOT NULL,
                                FOREIGN KEY (questionId) REFERENCES Question(questionId) ON DELETE CASCADE
);

#####################

CREATE TABLE `Level`(
                        levelId INT(10) UNSIGNED PRIMARY KEY NOT NULL,
                        `name` CHAR(20) NOT NULL
);

INSERT INTO `Level` SET
                        levelId = 1,
                        `name` = 'N1';

INSERT INTO `Level` SET
                        levelId = 2,
                        `name` = 'N2';

INSERT INTO `Level` SET
                        levelId = 3,
                        `name` = 'N3';

INSERT INTO `Level` SET
                        levelId = 4,
                        `name` = 'N4';

INSERT INTO `Level` SET
                        levelId = 5,
                        `name` = 'N5';

SELECT * FROM `Level`;

#####################

CREATE TABLE `Category`(
                           categoryId INT(10) UNSIGNED PRIMARY KEY AUTO_INCREMENT NOT NULL,
                           `name` CHAR(20) NOT NULL
);

INSERT INTO `Category` SET
    `name` = 'linguistic knowledge';

INSERT INTO `Category` SET
    `name` = 'reading';

INSERT INTO `Category` SET
    `name` = 'listening';

SELECT * FROM `Category`;

########################

# 회원 테이블 생성
CREATE TABLE `User`(
                       userId INT(10) UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
                       regDate DATETIME NOT NULL,
                       updateDate DATETIME NOT NULL,
                       loginId CHAR(30) NOT NULL,
                       loginPw CHAR(100) NOT NULL,
                       `authLevel` SMALLINT(2) UNSIGNED DEFAULT 3 COMMENT '권한 레벨 (3=일반, 7=관리자)',
                       `name` CHAR(20) NOT NULL,
                       nickname CHAR(20) NOT NULL,
                       cellphoneNum CHAR(20) NOT NULL,
                       email CHAR(50) NOT NULL,
                       delStatus TINYINT(1) UNSIGNED NOT NULL DEFAULT 0 COMMENT '탈퇴 여부 (0=탈퇴 전, 1=탈퇴 후)',
                       delDate DATETIME COMMENT '탈퇴 날짜'
);

CREATE TABLE `ExamSession`(
                              examSessionId INT(10) UNSIGNED PRIMARY KEY AUTO_INCREMENT NOT NULL,
                              userId INT(10) UNSIGNED NOT NULL,
                              `level` INT(10) UNSIGNED NOT NULL,
                              startTime DATETIME NOT NULL,
                              endTime DATETIME,
                              score INT(10) NOT NULL DEFAULT 0,
                              solvedQuestions INT(10) NOT NULL DEFAULT 0,
                              FOREIGN KEY (userId) REFERENCES `User`(userId) ON DELETE RESTRICT,
                              FOREIGN KEY (`level`) REFERENCES `Level`(levelId) ON DELETE RESTRICT
);

CREATE TABLE `ExamQuestionsPerSession`(
                                          examSessionQuestionId INT(10) UNSIGNED PRIMARY KEY AUTO_INCREMENT NOT NULL,
                                          examSessionId INT(10) UNSIGNED NOT NULL,
                                          questionId INT(10) UNSIGNED NOT NULL,
                                          FOREIGN KEY (examSessionId) REFERENCES `ExamSession`(examSessionId) ON DELETE CASCADE,
                                          FOREIGN KEY (questionId) REFERENCES `Question`(questionId) ON DELETE RESTRICT
);

CREATE TABLE `MockTestSession` (
                                   mockTestSessionId INT(10) UNSIGNED PRIMARY KEY AUTO_INCREMENT NOT NULL,
                                   userId INT(10) UNSIGNED NOT NULL,
                                   examYear INT(10) NOT NULL,
                                   examMonth INT(10) NOT NULL,
                                   `level` INT(10) UNSIGNED NOT NULL,
                                   startTime DATETIME NOT NULL,
                                   endTime DATETIME,
                                   score INT(10) NOT NULL DEFAULT 0,
                                   solvedQuestions INT(10) NOT NULL DEFAULT 0,
                                   FOREIGN KEY (userId) REFERENCES `User`(userId) ON DELETE RESTRICT,
                                   FOREIGN KEY (`level`) REFERENCES `Level`(levelId) ON DELETE RESTRICT
);

CREATE TABLE `MockTestQuestionsPerSession`(
                                              mockTestQuestionId INT(10) UNSIGNED PRIMARY KEY AUTO_INCREMENT NOT NULL,
                                              mockTestSessionId INT(10) UNSIGNED NOT NULL,
                                              questionId INT(10) UNSIGNED NOT NULL,
                                              FOREIGN KEY (mockTestSessionId) REFERENCES `MockTestSession`(mockTestSessionId) ON DELETE CASCADE,
                                              FOREIGN KEY (questionId) REFERENCES `Question`(questionId) ON DELETE RESTRICT
);


#회원 시험기록 테이블 생성
CREATE TABLE `UserExamRecord`(
                                 recordId INT(10) UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
                                 userId INT(10) UNSIGNED NOT NULL,
                                 examDate DATETIME NOT NULL,
                                 questionId INT(10) UNSIGNED NOT NULL,
                                 selectedAnswer INT(10),
                                 isCorrect BOOL NOT NULL,
                                 mockTestSessionId INT(10) UNSIGNED NULL,
                                 examSessionId INT(10) UNSIGNED NULL,
                                 FOREIGN KEY (userId) REFERENCES `User`(userId) ON DELETE RESTRICT,
                                 FOREIGN KEY (questionId) REFERENCES `Question`(questionId) ON DELETE RESTRICT,
                                 FOREIGN KEY (mockTestSessionId) REFERENCES `MockTestSession`(mockTestSessionId) ON DELETE CASCADE,
                                 FOREIGN KEY (examSessionId) REFERENCES `ExamSession`(examSessionId) ON DELETE CASCADE
);

#회원 시험기록 테이블 세션에 대한 트리거 생성
#INSERT
DELIMITER //

CREATE TRIGGER check_user_exam_record_insert
    BEFORE INSERT ON UserExamRecord
    FOR EACH ROW
BEGIN
    IF (NEW.mockTestSessionId IS NOT NULL AND NEW.examSessionId IS NOT NULL) OR
       (NEW.mockTestSessionId IS NULL AND NEW.examSessionId IS NULL) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'mockTestSessionId and examSessionId cannot both be NULL or both have values.';
    END IF;
END;
//

DELIMITER ;

#UPDATE
DELIMITER //

CREATE TRIGGER check_user_exam_record_update
    BEFORE UPDATE ON UserExamRecord
    FOR EACH ROW
BEGIN
    IF (NEW.mockTestSessionId IS NOT NULL AND NEW.examSessionId IS NOT NULL) OR
       (NEW.mockTestSessionId IS NULL AND NEW.examSessionId IS NULL) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'mockTestSessionId and examSessionId cannot both be NULL or both have values.';
    END IF;
END;
//

DELIMITER ;

SELECT *
FROM Question Q
         INNER JOIN Question_metadata QM
                    ON Q.questionId = QM.questionId
WHERE QM.level = 2 && QM.category = 1
ORDER BY RAND()
LIMIT 20;  -- 예시로 20개의 문제를 무작위로 선택
