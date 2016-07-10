package pku.abe.commons.profile;

public enum ProfileType {

    API {
        public String value() {
            return "API";
        }
    },
    SERVICE {
        public String value() {
            return "SERVICE";
        }
    },
    MC {
        public String value() {
            return "MC";
        }
    },
    REDIS {
        public String value() {
            return "REDIS";
        }
    },
    DB {
        public String value() {
            return "DB";
        }
    },
    HBASE {
        public String value() {
            return "HBASE";
        }
    },
    HBASEFAILFAST {
        public String value() {
            return "HBASEFAILFAST";
        }
    },
    HTTP {
        public String value() {
            return "HTTP";
        }
    },
    JVM {
        public String value() {
            return "JVM";
        }
    },
    APIFAILFAST {
        public String value() {
            return "APIFAILFAST";
        }
    },
    MCQ {
        public String value() {
            return "MCQ";
        }
    };

    public abstract String value();

}
