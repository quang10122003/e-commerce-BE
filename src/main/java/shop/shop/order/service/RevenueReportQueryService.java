// class chỉ đọc dữ liệu để phục vụ báo cáo doanh thu/ KPI cho admin.
package shop.shop.order.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import shop.shop.admin.Projection.DailyRevenueProjection;
import shop.shop.admin.dto.response.AdminComparisonSeries;
import shop.shop.admin.dto.response.AdminRevenueIn7day;
import shop.shop.admin.dto.response.AdminRevenueKpi;
import shop.shop.admin.dto.response.AdminRevenueRepone;
import shop.shop.admin.dto.response.AdminTrendSeries;
import shop.shop.common.OrderStatus;
import shop.shop.common.PeriodRange;
import shop.shop.common.PeriodType;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.order.repo.OrderRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RevenueReportQueryService {
    OrderRepository orderRepository;
    // định nghĩa format time
    static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static DateTimeFormatter DATE_FORMATTER_SHORT = DateTimeFormatter.ofPattern("dd/MM");

    // Lấy doanh thu của tuần trước.
    public BigDecimal getLastWeekRevenue() {
        LocalDate today = LocalDate.now();
        LocalDate startOfThisWeek = today.with(DayOfWeek.MONDAY);
        LocalDate startOfLastWeek = startOfThisWeek.minusWeeks(1);
        LocalDate endOfLastWeek = startOfThisWeek;

        return orderRepository.getRevenueByDay(
                startOfLastWeek.atStartOfDay(),
                endOfLastWeek.atStartOfDay());
    }

    // Lấy doanh thu của tuần hiện tại.
    public BigDecimal getWeekRevenue() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(7);

        return orderRepository.getRevenueByDay(
                startOfWeek.atStartOfDay(),
                endOfWeek.atStartOfDay());
    }

    // So sánh doanh thu hai tuần.
    public BigDecimal calculateGrowth(BigDecimal weekRevenue, BigDecimal lastWeekRevenue) {
        if (weekRevenue == null)
            weekRevenue = BigDecimal.ZERO;
        if (lastWeekRevenue == null)
            lastWeekRevenue = BigDecimal.ZERO;

        if (lastWeekRevenue.compareTo(BigDecimal.ZERO) == 0) {
            if (weekRevenue.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            return BigDecimal.valueOf(100);
        }

        BigDecimal diff = weekRevenue.subtract(lastWeekRevenue);
        BigDecimal ratio = diff.divide(lastWeekRevenue, 4, RoundingMode.HALF_UP);
        return ratio.multiply(BigDecimal.valueOf(100));
    }

    // Tính doanh thu từng ngày trong tuần.
    public List<AdminRevenueIn7day> getRevenueIn7Days() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        Map<LocalDate, Long> dailyMap = getDailyRevenueMap(OrderStatus.COMPLETED,
                new PeriodRange(startOfWeek.atStartOfDay(), endOfWeek.atTime(23, 59, 59)));

        List<AdminRevenueIn7day> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate currentDay = startOfWeek.plusDays(i);
            Long revenue = dailyMap.getOrDefault(currentDay, 0L);
            result.add(new AdminRevenueIn7day(BigDecimal.valueOf(revenue), currentDay.atStartOfDay()));
        }
        return result;
    }

    // lấy data cho trang thống kê doanh thu
    @Transactional(readOnly = true)
    public AdminRevenueRepone getRevenueData(PeriodType type, Integer year, Integer week, Integer month) {
        if (year == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Thiếu tham số year");
        }
        if (type == PeriodType.WEEK && week == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Thiếu tham số week");
        }
        if (type == PeriodType.MONTH && month == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Thiếu tham số month");
        }

        PeriodRange currentRange = getPeriodRange(type, year, week, month);
        PeriodRange previousRange = getPreviousPeriodRange(type, currentRange);

        AdminRevenueKpi totalRevenue = buildKpi(OrderStatus.COMPLETED, currentRange, previousRange);
        AdminRevenueKpi pendingRevenue = buildKpi(OrderStatus.PENDING, currentRange, previousRange);

        Map<String, AdminRevenueKpi> kpis = new HashMap<>();
        kpis.put("totalRevenue", totalRevenue);
        kpis.put("pending", pendingRevenue);

        Map<LocalDate, Long> currentDailyMap = getDailyRevenueMap(OrderStatus.COMPLETED, currentRange);
        Map<LocalDate, Long> previousDailyMap = getDailyRevenueMap(OrderStatus.COMPLETED, previousRange);

        List<LabeledPeriod> currentPeriods = groupByPeriod(currentDailyMap, currentRange, type);
        List<LabeledPeriod> previousPeriods = groupByPeriod(previousDailyMap, previousRange, type);

        List<AdminTrendSeries> trendSeries = currentPeriods.stream()
                .map(p -> new AdminTrendSeries(p.label(), p.total()))
                .toList();

        List<AdminComparisonSeries> comparisonSeries = new ArrayList<>();
        for (int i = 0; i < currentPeriods.size(); i++) {
            long previousTotal = i < previousPeriods.size() ? previousPeriods.get(i).total() : 0L;
            comparisonSeries.add(new AdminComparisonSeries(
                    currentPeriods.get(i).label(),
                    currentPeriods.get(i).total(),
                    previousTotal));
        }

        return AdminRevenueRepone.builder()
                .kpis(kpis)
                .trendSeries(trendSeries)
                .comparisonSeries(comparisonSeries)
                .build();
    }

    // Xác định khoảng thời gian (start, end) dựa trên loại kỳ và tham số.
    private PeriodRange getPeriodRange(PeriodType type, int year, Integer week, Integer month) {
        LocalDate startDate, endDate;

        switch (type) {
            case WEEK -> {
                LocalDate firstDayOfYear = LocalDate.ofYearDay(year, 1);
                LocalDate monday = firstDayOfYear.with(WeekFields.ISO.weekOfWeekBasedYear(), week)
                        .with(WeekFields.ISO.dayOfWeek(), 1);
                startDate = monday;
                endDate = monday.plusDays(6);
            }
            case MONTH -> {
                startDate = LocalDate.of(year, month, 1);
                endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
            }
            default -> {
                startDate = LocalDate.of(year, 1, 1);
                endDate = LocalDate.of(year, 12, 31);
            }
        }
        return new PeriodRange(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
    }

    private PeriodRange getPreviousPeriodRange(PeriodType type, PeriodRange current) {
        LocalDateTime start = current.getStart();
        LocalDateTime end = current.getEnd();
        long days = java.time.Duration.between(start, end).toDays() + 1;
        LocalDateTime prevStart = start.minusDays(days);
        LocalDateTime prevEnd = end.minusDays(days);
        return new PeriodRange(prevStart, prevEnd);
    }

    private AdminRevenueKpi buildKpi(OrderStatus status, PeriodRange current, PeriodRange previous) {
        BigDecimal currentValue = orderRepository.sumTotalAmountByStatusAndDateRange(status, current.getStart(),
                current.getEnd());
        BigDecimal previousValue = orderRepository.sumTotalAmountByStatusAndDateRange(status, previous.getStart(),
                previous.getEnd());

        if (currentValue == null)
            currentValue = BigDecimal.ZERO;
        if (previousValue == null)
            previousValue = BigDecimal.ZERO;

        Double deltaPct;
        if (previousValue.compareTo(BigDecimal.ZERO) == 0) {
            deltaPct = currentValue.compareTo(BigDecimal.ZERO) > 0 ? null : 0.0;
        } else {
            deltaPct = currentValue.subtract(previousValue)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(previousValue, 4, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return AdminRevenueKpi.builder()
                .value(currentValue)
                .deltaPct(deltaPct)
                .build();
    }

    private record LabeledPeriod(String label, long total) {
    }

    private List<LabeledPeriod> groupByPeriod(Map<LocalDate, Long> dailyMap, PeriodRange range, PeriodType type) {
        return switch (type) {
            case WEEK -> {
                List<LabeledPeriod> result = new ArrayList<>();
                LocalDate currentDate = range.getStart().toLocalDate();
                LocalDate endDate = range.getEnd().toLocalDate();
                while (!currentDate.isAfter(endDate)) {
                    String label = currentDate.format(DATE_FORMATTER);
                    result.add(new LabeledPeriod(label, dailyMap.getOrDefault(currentDate, 0L)));
                    currentDate = currentDate.plusDays(1);
                }
                yield result;
            }
            case MONTH -> {
                List<LabeledPeriod> result = new ArrayList<>();
                LocalDate start = range.getStart().toLocalDate();
                LocalDate end = range.getEnd().toLocalDate();
                LocalDate current = start;
                int weekNum = 1;
                while (!current.isAfter(end)) {
                    LocalDate weekEnd = current.plusDays(6);
                    if (weekEnd.isAfter(end))
                        weekEnd = end;
                    long weekTotal = 0;
                    for (LocalDate d = current; !d.isAfter(weekEnd); d = d.plusDays(1)) {
                        weekTotal += dailyMap.getOrDefault(d, 0L);
                    }
                    String label = String.format("Tuần %d (%s - %s)", weekNum,
                            current.format(DATE_FORMATTER_SHORT), weekEnd.format(DATE_FORMATTER_SHORT));
                    result.add(new LabeledPeriod(label, weekTotal));
                    current = weekEnd.plusDays(1);
                    weekNum++;
                }
                if (result.size() > 4) {
                    LabeledPeriod last = result.remove(result.size() - 1);
                    LabeledPeriod fourth = result.get(3);
                    result.set(3, new LabeledPeriod(fourth.label(), fourth.total() + last.total()));
                }
                yield result;
            }
            case YEAR -> {
                List<LabeledPeriod> result = new ArrayList<>();
                LocalDate start = range.getStart().toLocalDate();
                LocalDate end = range.getEnd().toLocalDate();
                LocalDate current = start;
                while (!current.isAfter(end)) {
                    LocalDate monthEnd = current.withDayOfMonth(current.lengthOfMonth());
                    if (monthEnd.isAfter(end))
                        monthEnd = end;
                    long monthTotal = 0;
                    for (LocalDate d = current; !d.isAfter(monthEnd); d = d.plusDays(1)) {
                        monthTotal += dailyMap.getOrDefault(d, 0L);
                    }
                    String label = String.format("Tháng %d", current.getMonthValue());
                    result.add(new LabeledPeriod(label, monthTotal));
                    current = monthEnd.plusDays(1);
                }
                yield result;
            }
            default -> throw new ApiError(ErrorCode.INVALID_PERIOD_TYPE);
        };
    }

    private Map<LocalDate, Long> getDailyRevenueMap(OrderStatus status, PeriodRange range) {
        List<DailyRevenueProjection> dailyData = orderRepository.getDailyRevenueByStatusAndDateRange(status,
                range.getStart(), range.getEnd());
        return dailyData.stream()
                .collect(Collectors.toMap(
                        DailyRevenueProjection::getDate,
                        DailyRevenueProjection::getTotal));
    }
}
