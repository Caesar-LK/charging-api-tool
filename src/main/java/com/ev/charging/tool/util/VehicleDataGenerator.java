package com.ev.charging.tool.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 车辆数据随机生成器。
 */
public class VehicleDataGenerator {

    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String[] PROVINCES = {"京", "沪", "津", "渝", "粤", "浙", "苏", "鲁", "晋", "冀",
            "豫", "湘", "鄂", "川", "陕", "辽", "吉", "黑", "闽", "赣"};
    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String PLATE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";

    private static final String[] NEV_SMALL_TYPES = {"纯电动轿车", "插电式混合动力轿车", "纯电动多用途乘用车"};
    private static final String[] NEV_LARGE_TYPES = {"纯电动重型货车", "纯电动半挂牵引车"};
    private static final String[] FUEL_VEHICLE_TYPES = {"重型货车", "小型货车", "轻型货车", "半挂牵引车", "仓栅式轻型货车"};

    private static final String[] NEV_SMALL_BRANDS = {"特斯拉ModelY", "比亚迪汉EV", "蔚来ES6", "小鹏P7", "理想L7",
            "比亚迪秦PLUSEV", "广汽埃安AIONY", "极氪001"};
    private static final String[] NEV_LARGE_BRANDS = {"比亚迪T8", "宇通E12", "远程星瀚H", "三一重卡", "徐工电动重卡"};
    private static final String[] FUEL_BRANDS = {"解放J6", "重汽豪沃", "东风天龙", "福田欧曼", "陕汽德龙"};

    private static final String[] DIMENSIONS_LARGE = {"6800x2500x3500mm", "7200x2400x3000mm",
            "7500x2550x3600mm", "8600x2500x3200mm"};
    private static final String[] DIMENSIONS_SMALL = {"4500x1800x1500mm", "4700x1835x1530mm",
            "4950x1860x1480mm", "5200x1900x1950mm", "5995x2200x3200mm"};

    private static final String[] NAMES = {"张三", "李四", "王五", "赵六", "孙七", "周八", "吴九", "郑十"};
    private static final String[] STREETS = {"人民路1号", "建设路88号", "青年大道66号", "解放街9号", "幸福路123号"};
    private static final String[] APPROVED_LOADS = {"2000kg", "3000kg", "5000kg", "8000kg", "10000kg", "15000kg"};
    private static final String[] TOWED_MASSES = {"10000kg", "15000kg", "20000kg", "25000kg", "30000kg"};
    private static final String[] REMARKS_LIST = {"", "危险品运输", "冷藏运输", "集装箱运输", "特种车辆"};
    private static final String[] NEV_REMARKS = {"新能源", "纯电动", "插电混动"};

    private static final String[] TRAFFIC_UNITS = {
            "广东省深圳市公安局交通警察支队", "广东省广州市公安局交通警察支队",
            "江苏省南京市公安局交通警察支队", "江苏省苏州市公安局交通警察支队",
            "浙江省杭州市公安局交通警察支队", "浙江省宁波市公安局交通警察支队",
            "山东省济南市公安局交通警察支队", "山东省青岛市公安局交通警察支队",
            "湖南省长沙市公安局交通警察支队", "四川省成都市公安局交通警察支队",
            "陕西省西安市公安局交通警察支队", "河北省石家庄市公安局交通警察支队",
            "河北省唐山市公安局交通警察支队", "湖北省武汉市公安局交通警察支队"
    };

    private static final String VIN_CHARS = "ABCDEFGHJKLMNPRSTUVWXYZ0123456789";
    private static final String VIN_YEAR_CODES = "ABCDEFGHJKLMNPRSTVWXY123456789";
    private static final String[] VIN_WMI = {"LFP", "LDC", "LSV", "L6T", "LE4", "LZW", "LBV",
            "LZG", "LVV", "LJD", "LGX", "LRD", "LFM", "LJ4", "LFV"};

    public static String generateVin() {
        String wmi = pick(VIN_WMI);
        StringBuilder vds = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            vds.append(VIN_CHARS.charAt(RANDOM.nextInt(VIN_CHARS.length())));
        }
        char check = "0123456789X".charAt(RANDOM.nextInt(11));
        char year = VIN_YEAR_CODES.charAt(RANDOM.nextInt(VIN_YEAR_CODES.length()));
        char plant = VIN_CHARS.charAt(RANDOM.nextInt(VIN_CHARS.length()));
        String serial = String.format("%06d", RANDOM.nextInt(1000000));
        return wmi + vds + check + year + plant + serial;
    }

    public static String generateNevPlate(boolean isLarge) {
        String province = pick(PROVINCES);
        char letter = LETTERS.charAt(RANDOM.nextInt(LETTERS.length()));
        String energyFlag = RANDOM.nextBoolean() ? "D" : "F";
        if (isLarge) {
            return province + letter + randomChars(PLATE_CHARS, 5) + energyFlag;
        } else {
            return province + letter + energyFlag + randomChars(PLATE_CHARS, 5);
        }
    }

    public static String generateFuelPlate() {
        return pick(PROVINCES)
                + LETTERS.charAt(RANDOM.nextInt(LETTERS.length()))
                + randomChars(PLATE_CHARS, 5);
    }

    public static VehicleData generateNevVehicle(boolean isLarge) {
        String vehicleType = isLarge ? pick(NEV_LARGE_TYPES) : pick(NEV_SMALL_TYPES);
        return buildVehicleData(vehicleType, isLarge, true);
    }

    public static VehicleData generateFuelVehicle() {
        String vehicleType = pick(FUEL_VEHICLE_TYPES);
        return buildVehicleData(vehicleType, true, false);
    }

    private static VehicleData buildVehicleData(String vehicleType, boolean isLarge, boolean isNev) {
        String plate = isNev ? generateNevPlate(isLarge) : generateFuelPlate();
        String vin = generateVin();
        String engine = "E" + (100000 + RANDOM.nextInt(900000));
        String name = pick(NAMES);

        int total = 10000 + RANDOM.nextInt(30000);
        int curb = Math.max(3000, (int) (total * (0.35 + RANDOM.nextDouble() * 0.25)));
        int seats = isLarge ? (2 + RANDOM.nextInt(2)) : (2 + RANDOM.nextInt(4));

        String dims = isLarge ? pick(DIMENSIONS_LARGE) : pick(DIMENSIONS_SMALL);
        String dimForApi = dims.replace("x", "×");

        LocalDate now = LocalDate.now();
        LocalDate regDate = now.minusDays(RANDOM.nextInt(365 * 4));
        LocalDate issueDate = regDate.plusDays(7 + RANDOM.nextInt(60));
        int inspYear = now.getYear() + 1 + RANDOM.nextInt(3);
        int inspMonth = 1 + RANDOM.nextInt(12);

        String archive = "A" + (100000 + RANDOM.nextInt(900000));
        String traffic = pick(TRAFFIC_UNITS);

        String numberColor;
        if (isNev) {
            numberColor = isLarge ? "黄绿" : "渐变绿";
        } else {
            numberColor = "蓝";
        }

        String brand;
        if (isNev) {
            brand = isLarge ? pick(NEV_LARGE_BRANDS) : pick(NEV_SMALL_BRANDS);
        } else {
            brand = pick(FUEL_BRANDS);
        }

        String remarks = isNev ? pick(NEV_REMARKS) : pick(REMARKS_LIST);

        return VehicleData.builder()
                .number(plate)
                .vehicleType(vehicleType)
                .name(name)
                .nameType("个人")
                .address("某市" + pick(STREETS))
                .useCharacter("非营运")
                .model(brand)
                .vin(vin)
                .engineNo(engine)
                .registerDate(regDate.format(DATE_FMT))
                .issueDate(issueDate.format(DATE_FMT))
                .fileNo(archive)
                .grossMass(total + "kg")
                .unladenMass(curb + "kg")
                .approvedLoad(isLarge ? pick(APPROVED_LOADS) : "")
                .approvedPassengers(isLarge ? "" : String.valueOf(seats))
                .dimension(dimForApi)
                .tractionMass(isLarge ? pick(TOWED_MASSES) : "")
                .inspectionRecord("检验有效期至" + inspYear + "年" + String.format("%02d", inspMonth) + "月")
                .issuingAuthority(traffic)
                .numberColor(numberColor)
                .frontUrl("id_front.jpg")
                .backUrl("id_back.jpg")
                .type("normal")
                .useVinCar(true)
                .remarks(remarks)
                .plateType(isNev ? (isLarge ? 2 : 3) : 1)
                .vehicleSource(1)
                .productType(1)
                .quota(1000)
                .price(0)
                .oldPrice(9.9)
                .build();
    }

    private static String pick(String[] arr) {
        return arr[RANDOM.nextInt(arr.length)];
    }

    private static String randomChars(String chars, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleData {
        private String number;
        private String vehicleType;
        private String name;
        private String nameType;
        private String address;
        private String useCharacter;
        private String model;
        private String vin;
        private String engineNo;
        private String registerDate;
        private String issueDate;
        private String fileNo;
        private String grossMass;
        private String unladenMass;
        private String approvedLoad;
        private String approvedPassengers;
        private String dimension;
        private String tractionMass;
        private String inspectionRecord;
        private String issuingAuthority;
        private String numberColor;
        private String frontUrl;
        private String backUrl;
        private String type;
        private boolean useVinCar;
        private String remarks;
        private int plateType;
        private int vehicleSource;
        private int productType;
        private int quota;
        private double price;
        private double oldPrice;

        public String toSaveJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"chargeVehicle\":{");
            sb.append("\"number\":\"").append(nullSafe(number)).append("\",");
            sb.append("\"vehicleType\":\"").append(nullSafe(vehicleType)).append("\",");
            sb.append("\"name\":\"").append(nullSafe(name)).append("\",");
            sb.append("\"nameType\":\"").append(nullSafe(nameType)).append("\",");
            sb.append("\"address\":\"").append(nullSafe(address)).append("\",");
            sb.append("\"useCharacter\":\"").append(nullSafe(useCharacter)).append("\",");
            sb.append("\"model\":\"").append(nullSafe(model)).append("\",");
            sb.append("\"vin\":\"").append(nullSafe(vin)).append("\",");
            sb.append("\"engineNo\":\"").append(nullSafe(engineNo)).append("\",");
            sb.append("\"registerDate\":\"").append(nullSafe(registerDate)).append("\",");
            sb.append("\"issueDate\":\"").append(nullSafe(issueDate)).append("\",");
            sb.append("\"fileNo\":\"").append(nullSafe(fileNo)).append("\",");
            sb.append("\"grossMass\":\"").append(nullSafe(grossMass)).append("\",");
            sb.append("\"unladenMass\":\"").append(nullSafe(unladenMass)).append("\",");
            sb.append("\"approvedLoad\":\"").append(nullSafe(approvedLoad)).append("\",");
            sb.append("\"approvedPassengers\":\"").append(nullSafe(approvedPassengers)).append("\",");
            sb.append("\"dimension\":\"").append(nullSafe(dimension)).append("\",");
            sb.append("\"tractionMass\":\"").append(nullSafe(tractionMass)).append("\",");
            sb.append("\"inspectionRecord\":\"").append(nullSafe(inspectionRecord)).append("\",");
            sb.append("\"issuingAuthority\":\"").append(nullSafe(issuingAuthority)).append("\",");
            sb.append("\"numberColor\":\"").append(nullSafe(numberColor)).append("\",");
            sb.append("\"frontUrl\":\"").append(nullSafe(frontUrl)).append("\",");
            sb.append("\"backUrl\":\"").append(nullSafe(backUrl)).append("\",");
            sb.append("\"type\":\"").append(nullSafe(type)).append("\",");
            sb.append("\"useVinCar\":").append(useVinCar).append(",");
            sb.append("\"remarks\":\"").append(nullSafe(remarks)).append("\"");
            sb.append("},");
            sb.append("\"financeInfo\":{");
            sb.append("\"plateNumber\":\"").append(nullSafe(number)).append("\",");
            sb.append("\"plateType\":").append(plateType).append(",");
            sb.append("\"vin\":\"").append(nullSafe(vin)).append("\",");
            sb.append("\"vehicleSource\":").append(vehicleSource).append(",");
            sb.append("\"productType\":").append(productType).append(",");
            sb.append("\"quota\":").append(quota).append(",");
            sb.append("\"price\":").append(price).append(",");
            sb.append("\"oldPrice\":").append(oldPrice);
            sb.append("}");
            sb.append("}");
            return sb.toString();
        }

        private static String nullSafe(String s) {
            return s == null ? "" : s;
        }
    }
}
