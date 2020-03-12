package com.blockfint;

import com.blockfint.schema.Labels;
import com.blockfint.schema.RelationshipTypes;
import org.codehaus.janino.ExpressionEvaluator;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.BranchState;

import java.util.Collections;
import java.util.Map;

public class DecisionTreeExpander implements PathExpander {
    private Map<String, String> facts;
    private ExpressionEvaluator ee = new ExpressionEvaluator();

    public DecisionTreeExpander(Map<String, String> facts) {
        this.facts = facts;
        ee.setExpressionType(boolean.class);
    }

    @Override
    public Iterable<Relationship> expand(Path path, BranchState branchState) {
        // If we get to an Answer stop traversing, we found a valid path.
        if (path.endNode().hasLabel(Labels.Answer)) {
            return Collections.emptyList();
        }

        // If we have Rules to evaluate, go do that.
        if (path.endNode().hasRelationship(Direction.OUTGOING, RelationshipTypes.HAS)) {
            return path.endNode().getRelationships(Direction.OUTGOING, RelationshipTypes.HAS);
        }

        // if (path.endNode().hasLabel(Labels.Rule)) {
        //     try {
        //         if (isTrue(path.endNode())) {
        //             return path.endNode().getRelationships(Direction.OUTGOING, RelationshipTypes.CASE_0);
        //         } else {
        //             return path.endNode().getRelationships(Direction.OUTGOING, RelationshipTypes.CASE_1);
        //         }
        //     } catch (Exception e) {
        //         // Could not continue this way!
        //         return Collections.emptyList();
        //     }
        // }

        if (path.endNode().hasLabel(Labels.Rule)) {
            try {
                String decision = getDecision(path.endNode());
                return path.endNode().getRelationships(Direction.OUTGOING, RelationshipTypes.valueOf(decision));
            } catch (Exception e) {
                // Could not continue this way!
                return Collections.emptyList();
            }
        }

        // Otherwise, not sure what to do really.
        return Collections.emptyList();
    }

    private String getDecision(Node rule) throws Exception {
        // Get the properties of the rule stored in the node
        Map<String, Object> ruleProperties = rule.getAllProperties();
        String[] parameterNames = Magic.explode((String) ruleProperties.get("parameter_names"));
        Class<?>[] parameterTypes = Magic.stringToTypes((String) ruleProperties.get("parameter_types"));

        // Fill the arguments array with their corresponding values
        Object[] arguments = new Object[parameterNames.length];
        for (int j = 0; j < parameterNames.length; j++) {
            arguments[j] = Magic.createObject(parameterTypes[j], facts.get(parameterNames[j]));
        }

        // Set our parameters with their matching types
        ee.setParameters(parameterNames, parameterTypes);

        String[] expressions = ((String) ruleProperties.get("expressions")).split(", ");

        for (int index = 0; index < expressions.length; index++) {
            // And now we "cook" (scan, parse, compile and load) the expression.
            ee.cook((String) expressions[index]);
            if ((boolean) ee.evaluate(arguments)) {
                return "CASE_" + index;
            }
        }

        // TODO: handle the default decision
        return "";
    }

    @Override
    public PathExpander reverse() {
        return null;
    }
}
