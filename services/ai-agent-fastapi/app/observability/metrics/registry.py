from collections import defaultdict
from threading import Lock


class MetricsRegistry:
    def __init__(self) -> None:
        self._lock = Lock()
        self._request_total = 0
        self._error_total = 0
        self._path_counts: dict[str, int] = defaultdict(int)
        self._latency_ms_total: dict[str, float] = defaultdict(float)
        self._agent_call_counts: dict[str, int] = defaultdict(int)
        self._agent_call_failures: dict[str, int] = defaultdict(int)
        self._tool_call_counts: dict[str, int] = defaultdict(int)
        self._tool_call_failures: dict[str, int] = defaultdict(int)

    def record_request(self, path: str, latency_ms: float, is_error: bool) -> None:
        with self._lock:
            self._request_total += 1
            self._path_counts[path] += 1
            self._latency_ms_total[path] += latency_ms
            if is_error:
                self._error_total += 1

    def snapshot(self) -> dict:
        with self._lock:
            per_path = []
            for path, count in self._path_counts.items():
                total = self._latency_ms_total[path]
                avg = total / count if count else 0.0
                per_path.append(
                    {
                        "path": path,
                        "request_count": count,
                        "avg_latency_ms": round(avg, 2),
                    }
                )
            return {
                "request_total": self._request_total,
                "error_total": self._error_total,
                "error_rate": round(self._error_total / self._request_total, 4)
                if self._request_total
                else 0.0,
                "paths": sorted(per_path, key=lambda item: item["path"]),
                "agent_calls": {
                    name: {
                        "count": self._agent_call_counts[name],
                        "failures": self._agent_call_failures[name],
                    }
                    for name in sorted(self._agent_call_counts.keys())
                },
                "tool_calls": {
                    name: {
                        "count": self._tool_call_counts[name],
                        "failures": self._tool_call_failures[name],
                    }
                    for name in sorted(self._tool_call_counts.keys())
                },
            }

    def record_agent_call(self, agent_name: str, success: bool) -> None:
        with self._lock:
            self._agent_call_counts[agent_name] += 1
            if not success:
                self._agent_call_failures[agent_name] += 1

    def record_tool_call(self, tool_name: str, success: bool) -> None:
        with self._lock:
            self._tool_call_counts[tool_name] += 1
            if not success:
                self._tool_call_failures[tool_name] += 1

    def to_prometheus_text(self) -> str:
        with self._lock:
            lines = [
                "# HELP patient_agent_http_requests_total Total number of HTTP requests",
                "# TYPE patient_agent_http_requests_total counter",
                f"patient_agent_http_requests_total {self._request_total}",
                "# HELP patient_agent_http_request_errors_total Total number of HTTP 5xx responses",
                "# TYPE patient_agent_http_request_errors_total counter",
                f"patient_agent_http_request_errors_total {self._error_total}",
                "# HELP patient_agent_http_path_requests_total Request count grouped by path",
                "# TYPE patient_agent_http_path_requests_total counter",
            ]

            for path, count in sorted(self._path_counts.items()):
                escaped_path = path.replace('"', '\\"')
                lines.append(
                    f'patient_agent_http_path_requests_total{{path="{escaped_path}"}} {count}'
                )

            lines.extend(
                [
                    "# HELP patient_agent_agent_calls_total Total agent invocations",
                    "# TYPE patient_agent_agent_calls_total counter",
                ]
            )
            for agent_name, count in sorted(self._agent_call_counts.items()):
                escaped_name = agent_name.replace('"', '\\"')
                lines.append(
                    f'patient_agent_agent_calls_total{{agent="{escaped_name}"}} {count}'
                )

            lines.extend(
                [
                    "# HELP patient_agent_agent_call_failures_total Total failed agent invocations",
                    "# TYPE patient_agent_agent_call_failures_total counter",
                ]
            )
            for agent_name, count in sorted(self._agent_call_failures.items()):
                escaped_name = agent_name.replace('"', '\\"')
                lines.append(
                    f'patient_agent_agent_call_failures_total{{agent="{escaped_name}"}} {count}'
                )

            lines.extend(
                [
                    "# HELP patient_agent_tool_calls_total Total tool invocations",
                    "# TYPE patient_agent_tool_calls_total counter",
                ]
            )
            for tool_name, count in sorted(self._tool_call_counts.items()):
                escaped_name = tool_name.replace('"', '\\"')
                lines.append(
                    f'patient_agent_tool_calls_total{{tool="{escaped_name}"}} {count}'
                )

            lines.extend(
                [
                    "# HELP patient_agent_tool_call_failures_total Total failed tool invocations",
                    "# TYPE patient_agent_tool_call_failures_total counter",
                ]
            )
            for tool_name, count in sorted(self._tool_call_failures.items()):
                escaped_name = tool_name.replace('"', '\\"')
                lines.append(
                    f'patient_agent_tool_call_failures_total{{tool="{escaped_name}"}} {count}'
                )

            return "\n".join(lines) + "\n"


metrics_registry = MetricsRegistry()
