/**
 * Copyright 2012 Twitter, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pku.abe.commons.useragent;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Java implementation of <a href="https://github.com/tobie/ua-parser">UA Parser</a>
 *
 * @author Steve Jiang (@sjiang) <gh at iamsteve com>
 */
public class Parser {
    private static final String REGEX_YAML_PATH = "regexes.yaml";
    private UserAgentParser uaParser;
    private OSParser osParser;
    private DeviceParser deviceParser;

    /**
     * construction function
     */

    public Parser() throws Exception{
        this(Parser.class.getClassLoader().getResourceAsStream(REGEX_YAML_PATH));
    }

    public Parser(InputStream regexYaml) {
        initialize(regexYaml);
    }

    public Client parse(String agentString) {
        UserAgent ua = parseUserAgent(agentString);
        OS os = parseOS(agentString);
        Device device = parseDevice(agentString);
        return new Client(ua, os, device);
    }

    public Client parseUA(String agentString) {
        List<UserAgent> ua = parseUserAgents(agentString);
        OS os = parseOS(agentString);
        Device device = parseDevice(agentString);
        return new Client(ua, os, device);
    }

    public UserAgent parseUserAgent(String agentString) {
        return uaParser.parse(agentString);
    }

    public List<UserAgent> parseUserAgents(String agentString) {
        return uaParser.parseUA(agentString);
    }

    public Device parseDevice(String agentString) {
        return deviceParser.parse(agentString);
    }

    public OS parseOS(String agentString) {
        return osParser.parse(agentString);
    }

    private void initialize(InputStream regexYaml) {
        Yaml yaml = new Yaml(new SafeConstructor());
        @SuppressWarnings("unchecked")
        Map<String,List<Map<String,String>>> regexConfig = (Map<String,List<Map<String,String>>>) yaml.load(regexYaml);

        List<Map<String,String>> uaParserConfigs = regexConfig.get("user_agent_parsers");
        if (uaParserConfigs == null) {
            throw new IllegalArgumentException("user_agent_parsers is missing from yaml");
        }
        uaParser = UserAgentParser.fromList(uaParserConfigs);

        List<Map<String,String>> osParserConfigs = regexConfig.get("os_parsers");
        if (osParserConfigs == null) {
            throw new IllegalArgumentException("os_parsers is missing from yaml");
        }
        osParser = OSParser.fromList(osParserConfigs);

        List<Map<String,String>> deviceParserConfigs = regexConfig.get("device_parsers");
        if (deviceParserConfigs == null) {
            throw new IllegalArgumentException("device_parsers is missing from yaml");
        }
        deviceParser = DeviceParser.fromList(deviceParserConfigs);
    }

    public static void main(String[] args) {
        try {
            Parser parser = new Parser();
            Client client1 = parser.parseUA("Mozilla/5.0 (Linux; Android 6.0.1; MI 5 Build/MXB48T) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/37.0.0.0 Mobile MQQBrowser/6.2 TBS/036524 Safari/537.36 MicroMessenger/6.3.18.800 NetType/WIFI Language/zh_CN");
            System.out.println(client1.userAgent);
            System.out.println(client1.os);
            System.out.println(client1.device);
            System.out.println("----");
            Client client2 = parser.parseUA("Mozilla/5.0 (Linux; U; Android 6.0.1; zh-cn; MI 5 Build/MXB48T) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/46.0.2490.85 Mobile Safari/537.36 XiaoMi/MiuiBrowser/8.0.9");
            System.out.println(client2.userAgent);
            System.out.println(client2.os);
            System.out.println(client2.device);

            System.out.println("----");
            Client client3 = parser.parseUA("Mozilla/5.0 (Linux; U; Android 5.1.1; zh-cn; YQ607 Build/LMY47V) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30 SogouMSE,SogouMobileBrowser/4.2.6");
            System.out.println(client3.userAgent);
            System.out.println(client3.os);
            System.out.println(client3.device);

            System.out.println("----");
            Client client4 = parser.parseUA("Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Mobile/13E238 MicroMessenger/6.3.16 NetType/WIFI Language/zh_CN");
            System.out.println(client4.userAgent);
            System.out.println(client4.os);
            System.out.println(client4.device);

            System.out.println("----");
            Client client5 = parser.parseUA("Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13E238 Safari/601.1");
            System.out.println(client5.userAgent);
            System.out.println(client5.os);
            System.out.println(client5.device);

            System.out.println("----");
            Client client6 = parser.parseUA(" Mozilla/5.0 (Linux; Android 5.1.1; KIW-AL10 Build/HONORKIW-AL10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.81 Mobile Safari/537.36");
            System.out.println(client6.userAgent);
            System.out.println(client6.os);
            System.out.println(client6.device);


            System.out.println("----");
            Client client7 = parser.parseUA(" Mozilla/5.0 (Linux; U; Android 4.4.2; zh-cn; MX4 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Mobile Safari/537.36");
            System.out.println(client7.userAgent);
            System.out.println(client7.os);
            System.out.println(client7.device);

            System.out.println("----");
            Client client8 = parser.parseUA("Mozilla/5.0 (Linux; U; Android 4.4.4; zh-CN; MX4 Pro Build/KTU84P) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 UCBrowser/10.10.3.810 U3/0.8.0 Mobile Safari/534.30");
            System.out.println(client8.userAgent);
            System.out.println(client8.os);
            System.out.println(client8.device);

            System.out.println("----");
            Client client9 = parser.parseUA("Mozilla/5.0 (Linux; U; Android 4.4.4; zh-CN; MX4 Pro Build/KTU84P) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 UCBrowser/10.10.3.810 U3/0.8.0 Mobile Safari/534.30");
            System.out.println(client9.userAgent);
            System.out.println(client9.os);
            System.out.println(client9.device);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
