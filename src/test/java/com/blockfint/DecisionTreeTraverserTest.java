package com.blockfint;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.test.server.HTTP;

import java.util.ArrayList;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static junit.framework.TestCase.assertEquals;

public class DecisionTreeTraverserTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String MODEL_STATEMENT = 
        "CREATE (tree:Tree { id: 'lending_decision' })"
           + "CREATE (income_rule:Rule { name: 'Income range of applicant', expressions: 'income < 30000, income >= 30000 && income <= 70000, income > 70000', parameter_names: 'income', parameter_types: 'int' })"
           + "CREATE (criminal_rule:Rule { name: 'Criminal record?', expressions: 'has_criminal_record, !has_criminal_record', parameter_names: 'has_criminal_record', parameter_types: 'boolean' })"
           + "CREATE (criminal_2_rule:Rule { name: 'Criminal record?', expressions: '!has_criminal_record, has_criminal_record', parameter_names: 'has_criminal_record', parameter_types: 'boolean' })"
           + "CREATE (job_rule:Rule { name: 'Years in present job?', expressions: 'years < 1, years >=1 && years <=5, years > 5', parameter_names: 'years', parameter_types: 'int' })"
           + "CREATE (credit_rule:Rule { name: 'Make credit card payments?', expressions: 'paid, !paid', parameter_names: 'paid', parameters_types: 'boolean' })"
           + "CREATE (approve_answer:Answer { id: 'approve' })"
           + "CREATE (reject_answer:Answer { id: 'reject' })"
           + "CREATE (tree)-[:HAS]->(income_rule)"
           + "CREATE (income_rule)-[:CASE_0]->(criminal_rule)"
           + "CREATE (criminal_rule)-[:CASE_0]->(approve_answer)"
           + "CREATE (criminal_rule)-[:CASE_1]->(reject_answer)"
           + "CREATE (income_rule)-[:CASE_1]->(job_rule)"
           + "CREATE (job_rule)-[:CASE_0]->(reject_answer)"
           + "CREATE (job_rule)-[:CASE_1]->(credit_rule)"
           + "CREATE (credit_rule)-[:CASE_0]->(approve_answer)"
           + "CREATE (credit_rule)-[:CASE_1]->(reject_answer)"
           + "CREATE (job_rule)-[:CASE_2]->(approve_answer)"
           + "CREATE (income_rule)-[:CASE_2]->(criminal_2_rule)"
           + "CREATE (criminal_2_rule)-[:CASE_0]->(approve_answer)"
           + "CREATE (criminal_2_rule)-[:CASE_1]->(reject_answer)";

    @Rule
    public final Neo4jRule neo4j = new Neo4jRule()
            .withFixture(MODEL_STATEMENT)
            .withProcedure(DecisionTreeTraverser.class);

    private static final Map QUERY1 =
            singletonMap("statements", singletonList(singletonMap("statement",
                    "CALL com.blockfint.traverse.decision_tree('lending_decision', {income: '1000000', has_criminal_record: 'false'}) yield path return path")));

    @Test
    public void testTraversal() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), QUERY1);
        int count = response.get("results").get(0).get("data").size();
        assertEquals(1, count);
        ArrayList<Map> path1 = mapper.convertValue(response.get("results").get(0).get("data").get(0).get("row").get(0), ArrayList.class);
        assertEquals("approve", path1.get(path1.size() - 1).get("id"));
    }

//     @Test
//     public void testTraversalTwo() throws Exception {
//         HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), QUERY2);
//         int count = response.get("results").get(0).get("data").size();
//         assertEquals(1, count);
//         ArrayList<Map> path1 = mapper.convertValue(response.get("results").get(0).get("data").get(0).get("row").get(0), ArrayList.class);
//         assertEquals("yes", path1.get(path1.size() - 1).get("id"));
//     }

//     private static final Map QUERY2 =
//             singletonMap("statements", singletonList(singletonMap("statement",
//                     "CALL com.blockfint.traverse.decision_tree('bar entrance', {gender:'female', age:'19'}) yield path return path")));

//     @Test
//     public void testTraversalThree() throws Exception {
//         HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), QUERY3);
//         int count = response.get("results").get(0).get("data").size();
//         assertEquals(1, count);
//         ArrayList<Map> path1 = mapper.convertValue(response.get("results").get(0).get("data").get(0).get("row").get(0), ArrayList.class);
//         assertEquals("yes", path1.get(path1.size() - 1).get("id"));
//     }

//     private static final Map QUERY3 =
//             singletonMap("statements", singletonList(singletonMap("statement",
//                     "CALL com.blockfint.traverse.decision_tree('bar entrance', {gender:'male', age:'23'}) yield path return path")));

}
