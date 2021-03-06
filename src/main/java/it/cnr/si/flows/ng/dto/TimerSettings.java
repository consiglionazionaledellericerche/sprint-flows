package it.cnr.si.flows.ng.dto;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * A timer.
 */
@Entity
@Table(name = "timer")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class TimerSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Column(name = "processInstanceId", nullable = false)
    private String processInstanceId;

    @NotNull
    @Column(name = "timerId", nullable = false)
    private String timerId;

    @NotNull
    @Column(name = "newDate", nullable = false)
    private Date newDate;

    @NotNull
    @Column(name = "yearAddValue", nullable = false)
    private int yearAddValue;

    @NotNull
    @Column(name = "monthAddValue", nullable = false)
    private int monthAddValue;

    @NotNull
    @Column(name = "dayAddValue", nullable = false)
    private int dayAddValue;

    @NotNull
    @Column(name = "hourAddValue", nullable = false)
    private int hourAddValue;

    @NotNull
    @Column(name = "minuteAddValue", nullable = false)
    private int minuteAddValue;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public TimerSettings processInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getTimerId() {
        return timerId;
    }

    public TimerSettings timerId(String timerId) {
        this.timerId = timerId;
        return this;
    }

    public void setTimerId(String timerId) {
        this.timerId = timerId;
    }


    public Date getNewDate() {
        return newDate;
    }

    public TimerSettings newDate(Date newDate) {
        this.newDate = newDate;
        return this;
    }

    public void setNewDate(Date newDate) {
        this.newDate = newDate;

    }

    public int getYearAddValue() {
        return yearAddValue;
    }

    public TimerSettings yearAddValue(int yearAddValue) {
        this.yearAddValue = yearAddValue;
        return this;
    }

    public void setYearAddValue(int yearAddValue) {
        this.yearAddValue = yearAddValue;
    }


    public int getMonthAddValue() {
        return yearAddValue;
    }

    public TimerSettings monthAddValue(int monthAddValue) {
        this.monthAddValue = monthAddValue;
        return this;
    }

    public void setMonthAddValue(int monthAddValue) {
        this.monthAddValue = monthAddValue;
    }

    public int getDayAddValue() {
        return dayAddValue;
    }

    public TimerSettings dayAddValue(int dayAddValue) {
        this.dayAddValue = dayAddValue;
        return this;
    }

    public void setDayAddValue(int dayAddValue) {
        this.dayAddValue = dayAddValue;
    }

    public int getHourAddValue() {
        return hourAddValue;
    }

    public TimerSettings hourAddValue(int hourAddValue) {
        this.hourAddValue = hourAddValue;
        return this;
    }

    public void setHourAddValue(int hourAddValue) {
        this.hourAddValue = hourAddValue;
    }

    public int getMinuteAddValue() {
        return minuteAddValue;
    }

    public TimerSettings minuteAddValue(int minuteAddValue) {
        this.minuteAddValue = minuteAddValue;
        return this;
    }

    public void setMinuteAddValue(int minuteAddValue) {
        this.minuteAddValue = minuteAddValue;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "timer{" +
                ", processInstanceId='" + processInstanceId + "'" +
                ", yearAddValue='" + yearAddValue + "'" +
                ", monthAddValue='" + monthAddValue + "'" +
                ", dayAddValue='" + dayAddValue + "'" +
                ", hourAddValue='" + hourAddValue + "'" +
                ", minuteAddValue='" + minuteAddValue + "'" +
                '}';
    }

}
