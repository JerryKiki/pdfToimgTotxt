package org.koreait;

public class MakeJLPTQuery {

    public Object doMakeJLPTQuery(String selectedSentence) {
        if (selectedSentence == null || selectedSentence.isEmpty()) {
          return null;
        }

        if(selectedSentence.startsWith("問題")) {
            Map<String, String> parentQuestionsQueries = makeQueryOfParentQuestions(selectedSentence);
        }
    }
}
