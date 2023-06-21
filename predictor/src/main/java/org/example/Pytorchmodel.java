package org.example;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.*;
import ai.onnxruntime.OnnxTensor;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Pytorchmodel {

    public static void main(String[] args) throws IOException, OrtException {

        String[] sentences = new String[]{"int <mask> = getAge();"};

        HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance(Paths.get("/home/vinoth/IdeaProjects/language-server-identifier-generator/predictor/artifacts/finetuned_albert/tokenizer.json"));

        Encoding[] encodings = tokenizer.batchEncode(sentences);

        int maskTokenIndex = -1;  // Initialize the mask token index

        for (int i = 0; i < encodings.length; i++) {
            String[] tokens = encodings[i].getTokens();
            for (int j = 0; j < tokens.length; j++) {
                tokens[j] = tokens[j].replace(" ", "").replace("Ġ", "").toLowerCase();
                System.out.print(tokens[j] + ", ");
                if (tokens[j].equals("<mask>")) {
                    maskTokenIndex = j;
                }
            }
        }

        if (maskTokenIndex == -1) {
            System.out.println("No masked token found in the sentence.");
            return;  // Exit if no masked token is found
        }
        System.out.println(maskTokenIndex);

        long[][] input_ids0 = new long[encodings.length][];
        long[][] attention_mask0 = new long[encodings.length][];

        for (int i = 0; i < encodings.length; i++) {
            input_ids0[i] = encodings[i].getIds();
            attention_mask0[i] = encodings[i].getAttentionMask();
        }

        OrtEnvironment environment = OrtEnvironment.getEnvironment();
        OrtSession session = environment.createSession("/home/vinoth/IdeaProjects/language-server-identifier-generator/predictor/artifacts/finetuned_albert/model1.onnx");

        OnnxTensor inputIds = OnnxTensor.createTensor(environment, input_ids0);
        OnnxTensor attentionMask = OnnxTensor.createTensor(environment, attention_mask0);

        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input_ids", inputIds);
        inputs.put("attention_mask", attentionMask);

        // Run the model
        OrtSession.Result outputs = session.run(inputs);

        // Get the predictions for the masked token
        Optional<OnnxValue> optionalValue = outputs.get("logits");
        OnnxTensor predictionsTensor = (OnnxTensor) optionalValue.get();
        float[][][] predictions = (float[][][]) predictionsTensor.getValue();
        int[] predictedTokenIndices = getTopKIndices(predictions[0][maskTokenIndex], 5); // Helper function to get top K indices

        // Get the masked value for the token
        long maskedTokenId = input_ids0[0][maskTokenIndex];
        String maskedToken = tokenizer.decode(new long[] { maskedTokenId });

        // Get the top predicted tokens
        String[] topPredictedTokens = new String[predictedTokenIndices.length];
        for (int i = 0; i < predictedTokenIndices.length; i++) {
            long predictedTokenId = predictedTokenIndices[i];
            String predictedToken = tokenizer.decode(new long[] { predictedTokenId });
            topPredictedTokens[i] = predictedToken;
        }

        // Print the masked value and top predicted tokens
        System.out.println("Masked Token: " + maskedToken);
        System.out.println("Top Predicted Tokens:");
        for (String token : topPredictedTokens) {
            System.out.println(token.replace(" ", "").replace("Ġ", "").toLowerCase());
        }
        System.out.println();
    }

    private static int[] getTopKIndices(float[] array, int k) {
        int[] indices = new int[k];
        for (int i = 0; i < k; i++) {
            int maxIndex = -1;
            float maxValue = Float.NEGATIVE_INFINITY;
            for (int j = 0; j < array.length; j++) {
                if (array[j] > maxValue) {
                    maxValue = array[j];
                    maxIndex = j;
                }
            }
            indices[i] = maxIndex;
            array[maxIndex] = Float.NEGATIVE_INFINITY;
        }
        return indices;
    }
}
