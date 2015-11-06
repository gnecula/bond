/**
 * Created by necula on 10/15/15.
 *
 * Code to support tabbed section in RST document.
 * Usage:
 *  
 *   .. container:: tab-section-group
 * 
 *      .. container:: tab-section-Python
 *
 *         Stuff that you want to show when the user selects Python
 *
 *      .. container:: tab-section-Other
 *
 *         Stuff that you want to show when the user selects Other
 *
 */
var TabSections = (function TabSections() {

    var self = {

        // Initialize the language sections. Find all sequences of high
        initialize: function () {
            // Scan all the tabbed-section

            var tab_container_idx = 0;
            var all_tabbed_containers = $('.container.tab-section-group');

            // List of CSS classes for all languages
            all_tabbed_containers.each(function (elemIdx, elem) {
                // Scan all the children that have a class tab-section-xxx
                tab_container_idx += 1;

                var tab_names = [];
                var tab_elements_to_remove = [];

                // Prepare the navigation DIV
                var nav_div = '<ul class="nav nav-tabs">';
                var nav_content = '<div class="tab-content">';

                $.each(elem.children, function (i, child) {
                    var classes = child.className.split(/\s+/);
                    $.each(classes, function (cidx, className) {
                        if(className.indexOf('tab-section-') == 0) {
                            var tab_name = className.substr(12);
                            var nav_div_class='';
                            var nav_content_class='';
                            if(tab_names.length == 0) {
                                nav_div_class=' class="active"';
                                nav_content_class=' in active';
                            }
                            tab_names.push(tab_name);

                            var tab_id = 'tab-'+tab_name+'-'+tab_container_idx;
                            nav_div += '<li '+nav_div_class+'><a data-toggle="tab" tab-name="'+tab_name+'" href="#'+tab_id+'">'+tab_name+'</a></li>';

                            nav_content += '<div id="'+tab_id+'" class="tab-pane fade'+nav_content_class+'">';
                            nav_content += child.outerHTML;
                            nav_content += '</div>';
                            tab_elements_to_remove.push(child);
                        }
                    });
                });
                nav_div += '</ul>';
                nav_content += '</div>';

                // Remove the elements we want to replace
                $.each(tab_elements_to_remove, function (i, child) {
                    $(child).remove();
                });

                $(elem).prepend(nav_content);
                $(elem).prepend(nav_div);

            }).on('shown.bs.tab', 'a[data-toggle="tab"]', function (ev) {
                self.change_tab($(ev.target).attr('tab-name'));
            });

            // Find the current tab
            var default_tab_section = window.localStorage ? window.localStorage.getItem('rst-tab-sections-current') : null;
            if(default_tab_section) {
                self.change_tab(default_tab_section);  // Default tab
            }
        },

        // Change the code language
        change_tab: function (new_tab, selector) {
            if(window.localStorage) {
                window.localStorage.setItem('rst-tab-sections-current', new_tab);
            }
            var all_tabbed_containers = $('.container.tab-section-group');

            // Find how far down the view is the selector element
            var initial_position_top = 0;
            if(selector) {
                initial_position_top = $(selector).offset().top - $(window).scrollTop();
            }

            // Show the proper tabs
            all_tabbed_containers.find('a[data-toggle="tab"][tab-name="'+new_tab+'"]').tab('show');

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
    window.TabSections.initialize();
});
