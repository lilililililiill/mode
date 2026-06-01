package com.agrieconomy.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 모드 설정 파일 정의.
 *
 * <p>서버 설정 (agrieconomy-server.toml):
 * <ul>
 *   <li>작물 등급 가중치 (SMALL / NORMAL / LARGE / PREMIUM)</li>
 * </ul>
 * </p>
 *
 * <p>공통 설정 (agrieconomy-common.toml):
 * <ul>
 *   <li>가격 재계산 기준 판매량</li>
 *   <li>자동 재해 발생 주기 (틱)</li>
 * </ul>
 * </p>
 */
public class AgriConfig {

    // ── 서버 설정 ─────────────────────────────────────────────────────

    public static final Server SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    static {
        ForgeConfigSpec.Builder serverBuilder = new ForgeConfigSpec.Builder();
        SERVER = new Server(serverBuilder);
        SERVER_SPEC = serverBuilder.build();
    }

    public static class Server {

        public final ForgeConfigSpec.IntValue gradeWeightSmall;
        public final ForgeConfigSpec.IntValue gradeWeightNormal;
        public final ForgeConfigSpec.IntValue gradeWeightLarge;
        public final ForgeConfigSpec.IntValue gradeWeightPremium;

        public Server(ForgeConfigSpec.Builder builder) {
            builder.comment("작물 등급 시스템 설정").push("crop_grade");

            gradeWeightSmall = builder
                    .comment("'작음' 등급 추첨 가중치 (기본 20)")
                    .defineInRange("weight_small", 20, 0, 100);

            gradeWeightNormal = builder
                    .comment("'보통' 등급 추첨 가중치 (기본 60)")
                    .defineInRange("weight_normal", 60, 0, 100);

            gradeWeightLarge = builder
                    .comment("'큼' 등급 추첨 가중치 (기본 17)")
                    .defineInRange("weight_large", 17, 0, 100);

            gradeWeightPremium = builder
                    .comment("'특급' 등급 추첨 가중치 (기본 3)")
                    .defineInRange("weight_premium", 3, 0, 100);

            builder.pop();
        }
    }

    // ── 공통 설정 ─────────────────────────────────────────────────────

    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();
        COMMON = new Common(commonBuilder);
        COMMON_SPEC = commonBuilder.build();
    }

    public static class Common {

        public final ForgeConfigSpec.IntValue marketResetSaleAmount;
        public final ForgeConfigSpec.BooleanValue disasterAutoEnabled;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.comment("거래소 설정").push("market");

            marketResetSaleAmount = builder
                    .comment("가격 기준점이 되는 판매량 (기본 1000)")
                    .defineInRange("reset_sale_amount", 1000, 100, 100000);

            builder.pop();

            builder.comment("재해 시스템 설정").push("disaster");

            disasterAutoEnabled = builder
                    .comment("자동 재해 발생 활성화 여부 (기본 false)")
                    .define("auto_enabled", false);

            builder.pop();
        }
    }
}
