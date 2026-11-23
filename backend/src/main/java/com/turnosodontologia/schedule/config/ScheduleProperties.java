package com.turnosodontologia.schedule.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.schedule")
public class ScheduleProperties {
    private String timezone = "America/Argentina/Buenos_Aires";
    private int slotDurationMinutes = 30;
    private PublicWindow publicWindow = new PublicWindow(24, 30);
    private List<DefaultWindow> defaultWindows = new ArrayList<>();

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public int getSlotDurationMinutes() { return slotDurationMinutes; }
    public void setSlotDurationMinutes(int slotDurationMinutes) { this.slotDurationMinutes = slotDurationMinutes; }
    public PublicWindow getPublicWindow() { return publicWindow; }
    public void setPublicWindow(PublicWindow publicWindow) { this.publicWindow = publicWindow; }
    public List<DefaultWindow> getDefaultWindows() { return defaultWindows; }
    public void setDefaultWindows(List<DefaultWindow> defaultWindows) { this.defaultWindows = defaultWindows; }

    public record PublicWindow(int minNoticeHours, int maxDays) {}
    public record DefaultWindow(String day, String start, String end) {}
}
