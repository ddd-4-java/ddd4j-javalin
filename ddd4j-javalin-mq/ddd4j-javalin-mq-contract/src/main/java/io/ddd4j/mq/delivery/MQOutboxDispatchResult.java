package io.ddd4j.mq.delivery;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

/**
 * 单次 Outbox 调度的汇总结果。
 *
 * 组件 claimed：已领取数量
 * 组件 published：已确认发布数量
 * 组件 rescheduled：已安排重试数量
 * 组件 dead：预计进入死信数量
 * 组件 confirmationLost：broker 已接收但租约确认丢失数量，后续可能重复投递
 */
public final class MQOutboxDispatchResult {

    private static final long serialVersionUID = 0L;

    private final int claimed;

    private final int published;

    private final int rescheduled;

    private final int dead;

    private final int confirmationLost;

    @JsonCreator()
    public MQOutboxDispatchResult(@JsonProperty("claimed") int claimed, @JsonProperty("published") int published, @JsonProperty("rescheduled") int rescheduled, @JsonProperty("dead") int dead, @JsonProperty("confirmationLost") int confirmationLost) {
        this.claimed = claimed;
        this.published = published;
        this.rescheduled = rescheduled;
        this.dead = dead;
        this.confirmationLost = confirmationLost;
    }

    @JsonProperty("claimed")
    public int claimed() {
        return claimed;
    }

    @JsonProperty("published")
    public int published() {
        return published;
    }

    @JsonProperty("rescheduled")
    public int rescheduled() {
        return rescheduled;
    }

    @JsonProperty("dead")
    public int dead() {
        return dead;
    }

    @JsonProperty("confirmationLost")
    public int confirmationLost() {
        return confirmationLost;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        MQOutboxDispatchResult other = (MQOutboxDispatchResult) obj;
        return this.claimed == other.claimed && this.published == other.published && this.rescheduled == other.rescheduled && this.dead == other.dead && this.confirmationLost == other.confirmationLost;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Integer.hashCode(claimed);
        result = 31 * result + Integer.hashCode(published);
        result = 31 * result + Integer.hashCode(rescheduled);
        result = 31 * result + Integer.hashCode(dead);
        result = 31 * result + Integer.hashCode(confirmationLost);
        return result;
    }

    @Override
    public String toString() {
        return "MQOutboxDispatchResult[claimed=" + claimed + ", published=" + published + ", rescheduled=" + rescheduled + ", dead=" + dead + ", confirmationLost=" + confirmationLost + "]";
    }
}
