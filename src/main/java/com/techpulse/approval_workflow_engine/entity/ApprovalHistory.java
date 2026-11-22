package com.techpulse.approval_workflow_engine.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Immutable
@Table(name = "approval_history")
@Getter // getters only, no setters
@Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
public class ApprovalHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "request_id", nullable = false, updatable = false)
	private Long requestId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private HistoryAction action;

	@Column(name = "action_by", nullable = false, updatable = false)
	private String actionBy;

	@Column(name = "action_at", nullable = false, updatable = false)
	private LocalDateTime actionAt;

	@PrePersist
	void onCreate() {
		if (this.actionAt == null) {
			this.actionAt = LocalDateTime.now();
		}
	}
}