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
            // List of CSS classes for all languages
            $('.container.code-examples').each(function (elem) {
                // Gather all the languages in the container
                var container_languages = [ 'python', 'ruby' ];
                // Insert the selector at the end
                var selector = ("<div class='code-language-selector-group'>Select the language for examples: " +
                    "<select class='code-language-selector'>"+
                    "<option value='python'>Python</option>" +
                    "<option value='ruby'>Ruby</option>" +
                    "</select>"+
                    "</div>");
                $(this).append(selector);
            }).on('change', '.code-language-selector', function (elem) {
                // We clicked on a code language selector
                var new_language = $(this).val();
                self.change_code_language(new_language);
            });

            self.change_code_language('python');  // Default language
        },

        // Change the code language
        change_code_language: function (new_language) {
             var all_code_example_containers = $('.container.code-examples');

            // Hide all the code fragments
            $.each(languages, function (i, lang) {
               all_code_example_containers.find('.highlight-'+lang).hide();
            });
            // Show the one we selected
            all_code_example_containers.find('.highlight-'+new_language).show();
            // Make sure all language selector use the new value
            all_code_example_containers.find('.code-language-selector').val(new_language);
        }
    };
    return self;
})();

$(document).ready(function () {
    window.BondDoc.initialize();
});