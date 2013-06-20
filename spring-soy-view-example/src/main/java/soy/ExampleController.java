package soy;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

@Controller
public class ExampleController {

	@RequestMapping(value="/")
	public String openHomepage(final Model model) {
		addServerTimeStringToModel(model);
		model.addAttribute("words", new ArrayList<String>());

		return "soy.index";
	}
	
	@RequestMapping(value="/server-time")
	public String getServerTime(Model model) {
		addServerTimeStringToModel(model);
		return "soy.serverTime";
	}

	private void addServerTimeStringToModel(Model model) {
		String serverTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		model.addAttribute("serverTime", serverTime);
	}

}
