package com.sp.user;

import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sp.common.XMLSerializer;

@Controller("user.userController")
public class UserController {
	@Autowired
	private UserService service;
	
	@Autowired
	private XMLSerializer xmlSerializer;
	
	@RequestMapping(value="/user/main")
	public String main() {
		return "user/main";
	}
	
	@RequestMapping(value="/user/jsonList1")
	@ResponseBody
	public Map<String, Object> jsonList1(HttpServletRequest req) throws Exception {
		Map<String, Object> model = null;
	
		String cp=req.getContextPath();
		String spec = req.getScheme()+"://"+req.getServerName()+":"+req.getServerPort()+cp;
		spec += "/xml/userXML.xml";
		
/*
    -- queryString이 존재하는 경우
        spec+="?search="+URLEncoder.encode(search, "UTF-8");		
 */
		
		model = service.serializeNode(spec);
		
		return model;
	}

	// produces 속성 : Response의 Content-Type을 제어
	                                                             // produces="text/json; charset=utf-8" 도 가능
	@RequestMapping(value="/user/jsonList2", produces="application/json; charset=utf-8")
	@ResponseBody
	public String jsonList2(HttpServletRequest req) throws Exception {
		String resultJSON = null;
	
		String cp=req.getContextPath();
		String spec = req.getScheme()+"://"+req.getServerName()+":"+req.getServerPort()+cp;
		spec += "/xml/userXML.xml";
		
		resultJSON = xmlSerializer.xmlToJson(spec);
		
		return resultJSON;
	}
	
	@RequestMapping(value="/user/xmlList", produces="application/xml; charset=utf-8")
	@ResponseBody
	public String xmlList(HttpServletRequest req) throws Exception {
		String resultXML = null;
	
		String cp=req.getContextPath();
		String spec = req.getScheme()+"://"+req.getServerName()+":"+req.getServerPort()+cp;
		spec += "/xml/userXML.xml";
		
		resultXML = xmlSerializer.xmlToString(spec);
		// resultXML = service.documentWriter(spec);
		
		return resultXML;
	}
	
	@RequestMapping(value="/user/goData", produces="application/json; charset=utf-8")
	@ResponseBody
	public String goData(HttpServletRequest req) throws Exception {
		String result=null;
		
		// 도로명 주소 조회 서비스 - 서비스 신청 후 약 1시간 후
		
		String keyword="세종로 17";
		String serviceKey = "ZqzcEwSo%2BJPXcUcX%2Bv8S%2F74RFluh6DFRFiLlCSwUtN%2Fr1S7PuFpdg8dAcQKKlMrfHXw%2FqxnofNWaFzWs%2B8jJWQ%3D%3D";
		String spec="http://openapi.epost.go.kr/postal/retrieveNewAdressAreaCdService/retrieveNewAdressAreaCdService/getNewAddressListAreaCd"
				+ "?ServiceKey="+serviceKey+"&searchSe=road&srchwrd="+URLEncoder.encode( keyword, "UTF-8");
		
		result = xmlSerializer.xmlToJson(spec);
		return result;
	}
}
