package ch.swisstopo.oerebchecker.utils;

public final class EnvVars {
    private EnvVars() {}

    public static final String S3_REGION_NAME = "S3RegionName";
    public static final String S3_ACCESS_KEY = "S3AccessKey";
    public static final String S3_SECRET_KEY = "S3SecretKey";
    public static final String S3_SCRIPTS_BUCKET = "SCRIPTS_BUCKET";
    public static final String S3_SCRIPTS_CONFIG_KEY = "S3Config";
    public static final String S3_RESULTS_BUCKET = "RESULTS_BUCKET";
    public static final String S3_RESULTS_OUTPUT_PATH = "S3ResultOutputPath";

    public static final String BFS_UID_PARTNER_LOGIN = "BFS_UID_PARTNER_LOGIN";
    public static final String BFS_UID_PARTNER_PASSWORD = "BFS_UID_PARTNER_PASSWORD";

    public static final String MAX_IMAGE_ASPECT_RATIO_PERCENTAGE_DIFFERENCE = "MaxImageAspectRatioPercentageDifference";

    public static final String OUTPUT_PATH = "OUTPUT_PATH";
}
