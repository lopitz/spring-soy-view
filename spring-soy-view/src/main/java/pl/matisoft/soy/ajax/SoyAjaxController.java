package pl.matisoft.soy.ajax;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.template.soy.msgs.SoyMsgBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpClientErrorException;
import pl.matisoft.soy.bundle.EmptySoyMsgBundleResolver;
import pl.matisoft.soy.bundle.SoyMsgBundleResolver;
import pl.matisoft.soy.compile.EmptyTofuCompiler;
import pl.matisoft.soy.compile.TofuCompiler;
import pl.matisoft.soy.locale.EmptyLocaleProvider;
import pl.matisoft.soy.locale.LocaleProvider;
import pl.matisoft.soy.template.EmptyTemplateFilesResolver;
import pl.matisoft.soy.template.TemplateFilesResolver;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Controller
public class SoyAjaxController {

    private static final Logger logger = LoggerFactory.getLogger(SoyAjaxController.class);

    private String cacheControl = "public, max-age=3600";

	private ConcurrentHashMap<File, String> cachedJsTemplates = new ConcurrentHashMap<File, String>();

    private TemplateFilesResolver templateFilesResolver = new EmptyTemplateFilesResolver();

    private TofuCompiler tofuCompiler = new EmptyTofuCompiler();

    private SoyMsgBundleResolver soyMsgBundleResolver = new EmptySoyMsgBundleResolver();

    private LocaleProvider localeProvider = new EmptyLocaleProvider();

    private boolean debugOn = false;

	public SoyAjaxController() {
	}

    @RequestMapping(value="/soy/{templateFileName}.js", method=GET)
	public ResponseEntity<String> getJsForTemplateFile(@PathVariable String templateFileName, final HttpServletRequest request) throws IOException {
        Preconditions.checkNotNull(templateFilesResolver, "templateFilesResolver cannot be null");

        final Optional<File> templateFile = templateFilesResolver.resolve(templateFileName);

		if (!debugOn && cachedJsTemplates.containsKey(templateFile.get())) {
            logger.debug("Debug off and returning cached compiled file:" + templateFile.get());
			return prepareResponseFor(cachedJsTemplates.get(templateFile.get()));
		}

        logger.debug("Compiling JavaScript template:" + templateFile.orNull());

        if (!templateFile.isPresent()) {
            throw notFound("File not found:" + templateFileName + ".soy");
        }

        final String templateContent = compileTemplateAndAssertSuccess(request, templateFile);
        if (!debugOn) {
            if (templateFile.isPresent()) {
                logger.debug("Debug off adding to templateFile to cache:" + templateFile.get());
                cachedJsTemplates.putIfAbsent(templateFile.get(), templateContent);
            }
        }

        return prepareResponseFor(templateContent);
    }

	private ResponseEntity<String> prepareResponseFor(final String templateContent) {
		final HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "text/javascript");
		headers.add("Cache-Control", debugOn ? "no-cache" : cacheControl);

		return new ResponseEntity<String>(templateContent, headers, OK);
	}

	private String compileTemplateAndAssertSuccess(final HttpServletRequest request, Optional<File> templateFile) throws IOException {
        Preconditions.checkNotNull(localeProvider, "localeProvider cannot be null");
        Preconditions.checkNotNull(soyMsgBundleResolver, "soyMsgBundleResolver cannot be null");
        Preconditions.checkNotNull(tofuCompiler, "tofuCompiler cannot be null");

        final Optional<Locale> locale = localeProvider.resolveLocale(request);
        final Optional<SoyMsgBundle> soyMsgBundle = soyMsgBundleResolver.resolve(locale);
		final List<String> compiledTemplates = tofuCompiler.compileToJsSrc(templateFile.orNull(), soyMsgBundle.orNull());

        final Iterator it = compiledTemplates.iterator();
		if (!it.hasNext()) {
			throw notFound("No compiled templates found!");
		}

		return (String) it.next();
	}

	private HttpClientErrorException notFound(String file) {
		return new HttpClientErrorException(NOT_FOUND, file);
	}

	public void setCacheControl(final String cacheControl) {
		this.cacheControl = cacheControl;
	}

    public void setTemplateFilesResolver(final TemplateFilesResolver templateFilesResolver) {
        this.templateFilesResolver = templateFilesResolver;
    }

    public void setTofuCompiler(final TofuCompiler tofuCompiler) {
        this.tofuCompiler = tofuCompiler;
    }

    public void setSoyMsgBundleResolver(SoyMsgBundleResolver soyMsgBundleResolver) {
        this.soyMsgBundleResolver = soyMsgBundleResolver;
    }

    public void setLocaleProvider(LocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    public void setDebugOn(boolean debugOn) {
        this.debugOn = debugOn;
    }

}