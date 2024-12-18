package jsonParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Parser {

    private static final String POPULAR_PRODUCTS_FILE = "popular_products.csv";
    private static final String RATED_PRODUCTS_FILE = "rated_products.csv";
    private static final String POPULAR_PRODUCTS_PERIOD_FILE = "popular_products_period.csv";
    private static final String MATCHED_PRODUCTS_FILE = "matched_products.csv";

    public static void main(String[] args) throws IOException, ParseException {

        String filePath = args[0];
        List<JsonNode> reviews = loadReviews(filePath);

        Scanner scanner = new Scanner(System.in);

        Map<String, Long> productPopularity = reviews.stream()
                .collect(Collectors.groupingBy(review -> review.get("asin").asText(), Collectors.counting()));

        List<Map.Entry<String, Long>> sortedByPopularity = productPopularity.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .collect(Collectors.toList());

        writePopularProducts(sortedByPopularity, reviews, POPULAR_PRODUCTS_FILE);
        System.out.println("Популярные продукты записаны в " + POPULAR_PRODUCTS_FILE);

        Map<String, Double> productRatings = reviews.stream()
                .collect(Collectors.groupingBy(review -> review.get("asin").asText(),
                        Collectors.averagingDouble(review -> review.get("overall").asDouble())));

        List<Map.Entry<String, Double>> sortedByRating = productRatings.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .collect(Collectors.toList());

        writeRatedProducts(sortedByRating, reviews, RATED_PRODUCTS_FILE);
        System.out.println("Продукты по рейтингу записаны в " + RATED_PRODUCTS_FILE);

        System.out.println("Введите начальную дату (MM dd, yyyy):");
        String startDate = scanner.nextLine();
        System.out.println("Введите конечную дату (MM dd, yyyy):");
        String endDate = scanner.nextLine();

        List<JsonNode> filteredByPeriod = filterReviewsByDate(reviews, startDate, endDate);

        Map<String, Long> popularProductsInPeriod = filteredByPeriod.stream()
                .collect(Collectors.groupingBy(review -> review.get("asin").asText(), Collectors.counting()));

        List<Map.Entry<String, Long>> sortedByPopularityInPeriod = popularProductsInPeriod.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .collect(Collectors.toList());

        writePopularProductsPeriod(sortedByPopularityInPeriod, reviews, POPULAR_PRODUCTS_PERIOD_FILE);
        System.out.println("Популярные продукты за период записаны в " + POPULAR_PRODUCTS_PERIOD_FILE);

        System.out.println("Введите текст для поиска в отзывах:");
        String searchTerm = scanner.nextLine();

        List<JsonNode> matchedReviews = reviews.stream()
                .filter(review -> review.has("reviewText") &&
                        review.get("reviewText").asText().toLowerCase().contains(searchTerm.toLowerCase()))
                .collect(Collectors.toList());

        writeMatchedProducts(matchedReviews, MATCHED_PRODUCTS_FILE);
        System.out.println("Совпадающие продукты записаны в " + MATCHED_PRODUCTS_FILE);

        System.out.println("Парсинг данных успешно завершен и экспортирован в CSV файлы.");
    }

    private static List<JsonNode> loadReviews(String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<JsonNode> reviews = new ArrayList<>();
        Files.lines(Paths.get(filePath)).forEach(line -> {
            try {
                reviews.add(mapper.readTree(line));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return reviews;
    }

    private static void writePopularProducts(List<Map.Entry<String, Long>> sortedByPopularity, List<JsonNode> reviews,
                                             String fileName) throws IOException {
        Map<String, JsonNode> uniqueProducts = reviews.stream()
                .collect(Collectors.toMap(
                        review -> review.get("asin").asText(),
                        review -> review,
                        (existing, replacement) -> existing));

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName))) {
            writer.write("ASIN,КоличествоОтзывов,Verified,Style");
            writer.newLine();
            for (Map.Entry<String, Long> entry : sortedByPopularity) {
                JsonNode product = uniqueProducts.get(entry.getKey());
                writer.write(entry.getKey() + "," + entry.getValue() + "," +
                        product.get("verified").asBoolean() + "," + getStyleString(product.get("style")));
                writer.newLine();
            }
        }
    }

    private static void writeRatedProducts(List<Map.Entry<String, Double>> sortedByRating,
                                           List<JsonNode> reviews, String fileName) throws IOException {
        Map<String, JsonNode> uniqueProducts = reviews.stream()
                .collect(Collectors.toMap(
                        review -> review.get("asin").asText(),
                        review -> review,
                        (existing, replacement) -> existing));

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName))) {
            writer.write("ASIN,AvgRating,Verified,Style");
            writer.newLine();
            for (Map.Entry<String, Double> entry : sortedByRating) {
                JsonNode product = uniqueProducts.get(entry.getKey());
                writer.write(entry.getKey() + "," + entry.getValue() + "," +
                        product.get("verified").asBoolean() + "," + getStyleString(product.get("style")));
                writer.newLine();
            }
        }
    }

    private static void writePopularProductsPeriod(List<Map.Entry<String, Long>> sortedByPopularityInPeriod,
                                                   List<JsonNode> reviews, String fileName) throws IOException {
        Map<String, JsonNode> uniqueProducts = reviews.stream()
                .collect(Collectors.toMap(
                        review -> review.get("asin").asText(),
                        review -> review,
                        (existing, replacement) -> existing));

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName))) {
            writer.write("ASIN,ReviewsCount,Verified,Style");
            writer.newLine();
            for (Map.Entry<String, Long> entry : sortedByPopularityInPeriod) {
                JsonNode product = uniqueProducts.get(entry.getKey());
                writer.write(entry.getKey() + "," + entry.getValue() + "," +
                        product.get("verified").asBoolean() + "," + getStyleString(product.get("style")));
                writer.newLine();
            }
        }
    }

    private static void writeMatchedProducts(List<JsonNode> matchedReviews, String fileName) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName))) {
            writer.write("ASIN,ReviewText,Verified,Style");
            writer.newLine();
            Set<String> uniqueAsins = new HashSet<>();
            for (JsonNode review : matchedReviews) {
                if (uniqueAsins.add(review.get("asin").asText())) {
                    writer.write(review.get("asin").asText() + "," +
                            review.get("reviewText").asText() + "," +
                            review.get("verified").asBoolean() + "," +
                            getStyleString(review.get("style")));
                    writer.newLine();
                }
            }
        }
    }

    private static List<JsonNode> filterReviewsByDate(List<JsonNode> reviews, String startDate, String endDate)
            throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM dd, yyyy");
        long start = dateFormat.parse(startDate).getTime();
        long end = dateFormat.parse(endDate).getTime();

        return reviews.stream()
                .filter(review -> {
                    long reviewTime = review.get("unixReviewTime").asLong() * 1000;
                    return reviewTime >= start && reviewTime <= end;
                })
                .collect(Collectors.toList());
    }

    private static String getStyleString(JsonNode styleNode) {
        if (styleNode == null) {
            return "";
        }

        StringBuilder styleBuilder = new StringBuilder();
        styleNode.fields().forEachRemaining(entry -> styleBuilder.append(entry.getKey())
                .append(entry.getValue().asText())
                .append(", "));
        if (styleBuilder.length() > 0) {
            styleBuilder.setLength(styleBuilder.length() - 2); 
        }
        return styleBuilder.toString();
    }
}
