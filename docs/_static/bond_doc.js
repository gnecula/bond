/**
 * Created by necula on 10/15/15.
 */
var BondDoc = (function BondDoc() {

    // The languages we support
    var languages = [ 'python', 'ruby' ];

    var self = {
        // Select the given language
        selectLanguage: function (language) {
            $.each(languages, function (i, lang) {
                var codeSections = $('.highlight-' + lang);
                if (lang == language) {
                    codeSections.show();
                } else {
                    codeSections.hide();
                }
            });
        },

        // Initialize the language sections. Find all sequences of high
        initialize: function () {
            // Scan all the highlight-xxx and add the class language-xxx because that is
            // the general mechanism
            var all_code_example_containers = $('.container.code-examples');
            $.each(languages, function (i, lang) {
                all_code_example_containers.find('.highlight-' + lang).addClass('code-language-' + lang);
            });

            // List of CSS classes for all languages
            all_code_example_containers.each(function (elem) {
                // Gather all the languages in the container
                var container_languages = ['python', 'ruby'];
                // Insert the selector at the end
                var selector = ("<div class='code-language-selector-group'><div class='code-language-selector-inner'>" +
                "Code examples in: " +
                "<select class='code-language-selector'>" +
                "<option value='python'>Python</option>" +
                "<option value='ruby'>Ruby</option>" +
                "</select>" +
                "</div></div>");
                $(this).append(selector);
            }).on('change', '.code-language-selector', function (elem) {
                // We clicked on a code language selector
                var new_language = $(this).val();
                self.change_code_language(new_language, this);
            });

            // Find the language
            var default_language = window.localStorage ? window.localStorage.getItem('bond-default-language') : null;
            if(! default_language) {
                default_language = 'python';
            }
            self.change_code_language(default_language);  // Default language
        },

        // Change the code language
        change_code_language: function (new_language, selector) {
            if(window.localStorage) {
                window.localStorage.setItem('bond-default-language', new_language);
            }
            var all_code_example_containers = $('.container.code-examples');

            // Find how far down the view is the selector element
            var initial_position_top = 0;
            if(selector) {
                initial_position_top = $(selector).offset().top - $(window).scrollTop();
            }

            // Hide all the code fragments
            $.each(languages, function (i, lang) {
                all_code_example_containers.find('.code-language-' + lang).hide();
            });
            // Show the one we selected
            all_code_example_containers.find('.code-language-' + new_language).show();
            // Make sure all language selector use the new value
            all_code_example_containers.find('.code-language-selector').val(new_language);

            // Find the new scroll position
            if(selector) {
                var final_position = $(selector).offset().top;
                $(window).scrollTop(final_position - initial_position_top);
            }
        }
    };
    return self;
})();

$(document).ready(function () {
    window.BondDoc.initialize();
});