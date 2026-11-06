package com.smarten.module.datalake.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "桑基图（能耗流向）")
public class SankeyGraphVO {

    private List<Node> nodes = new ArrayList<>();

    private List<Link> links = new ArrayList<>();

    @Data
    public static class Node {
        private String name;

        private Integer depth;

        public Node() {
        }

        public Node(String name, Integer depth) {
            this.name = name;
            this.depth = depth;
        }
    }

    @Data
    public static class Link {
        private String source;

        private String target;

        private BigDecimal value;
    }
}
