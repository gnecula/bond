require_relative '../bond'
require 'shellwords'

class String
  def bold
    "\e[1m#{self}\e[0m"
  end
end

# Utility methods for use by Bond.
# @api private
class Utils
  extend BondTargetable

  # Convert an observation object into a JSON string, serializing
  # decimals with precision `decimal_precision`.
  def self.observation_to_json(observation, decimal_precision = 9)
    JSON.neat_generate(observation, sorted: true, decimals: decimal_precision,
                       indent: ' '*4, wrap: true, after_colon: 1)
  end

  # Convert a JSON string into an observation object.
  def self.observation_from_json(json)
    JSON.parse!(json, symbolize_names: true)
  end

  # Convert an object to JSON and back to ensure it will match exactly with
  # something else that went through the same process.
  def self.observation_json_serde(observation)
    observation_from_json(observation_to_json(observation))
  end

  # Display a message to the user, via the console if `BOND_RECONCILE` is
  # 'console' and a stdin stream is available, else using a dialog box.
  # Show some main content (`content`) with shorter strings before
  # (`before_prompt`) and after (`after_prompt`). The content is optional,
  # and only one prompt needs to be specified. `options` is an array of strings
  # which will be displayed to the user as options to choose from; the last
  # option is the default. If the input is gathered via the console,
  # `single_char_options` will be used as the single-character version of
  # each option. The two arrays must be the same length and in the same order,
  # e.g. you might specify `options = ['accept', 'deny']` and
  # `single_char_options = ['a', 'd']`. The character *must* appear somewhere
  # in the option. Returns whichever option was chosen (guaranteed to be equal
  # to one of the options).
  bond.spy_point(mock_only: true)
  def self.get_user_input(before_prompt, after_prompt, content, options, single_char_options)
    if Bond.instance.reconcile_mode == 'console' and STDIN.tty?
      get_user_input_console(before_prompt, after_prompt, content, options, single_char_options)
    else
      get_user_input_dialog(before_prompt, after_prompt, content, options)
    end
  end

  # Same as {Utils.get_user_input} but allows editing of the `content` block.
  # Instead of just returning the option chosen, returns:
  # "option_chosen \n content_including_edits"
  # Also only supports dialog editing, not console.
  # TODO ETK support console editing by using some system editor
  bond.spy_point(mock_only: true)
  def self.get_user_input_with_edits(before_prompt, after_prompt, content, options)
    cmd = "#{Shellwords.shellescape(Bond::BOND_DIALOG_SCRIPT)} " +
        "--before-prompt #{Shellwords.shellescape(before_prompt)} " +
        "--after-prompt #{Shellwords.shellescape(after_prompt)} " +
        "--content #{Shellwords.shellescape(content)} " +
        "--editable-content " +
        "#{options.map { |opt| Shellwords.shellescape(opt) }.join(' ')}"
    ret = `#{cmd}`
    ret[-1] == "\n" ? ret.slice(0, ret.length - 1) : ret
    input = ret.split("\n")[0]
    content = ret.split("\n")[1..-1].join("\n")
    return input, content
  end

  private

  # Implement `get_user_input` using console interaction.
  def self.get_user_input_console(before_prompt, after_prompt, content, options, single_char_options)
    opts_with_single_char = options.zip(single_char_options).map { |opt, char| opt.sub(char, "[#{char}]") }
    # Default option, highlight it in bold
    opts_with_single_char[-1] = opts_with_single_char[-1].bold
    if before_prompt != '' and after_prompt == ''
      # Switch them so that after_prompt can be printed with the option string
      after_prompt = before_prompt
      before_prompt = ''
    end
    puts before_prompt.bold if before_prompt != ''
    puts content if content != ''
    puts "#{after_prompt.bold} (#{opts_with_single_char.join(' | ')}):"
    response = STDIN.gets.chomp
    return options[-1] if response.length == 0 # No input; return the default
    if response.length == 1 # Single char; find matching option
      possible = options.zip(single_char_options).select { |opt, char| char == response }
      return options[-1] if possible.length == 0 # Return default if no match found
      return possible.first.first
    end
    return response if options.include?(response) # Return response only if it was an option
    return options[-1] # Otherwise return default
  end

  # Implement `get_user_input` using a dialog window.
  def self.get_user_input_dialog(before_prompt, after_prompt, content, options)
    cmd = "#{Shellwords.shellescape(Bond::BOND_DIALOG_SCRIPT)} " +
        "--before-prompt #{Shellwords.shellescape(before_prompt)} " +
        "--after-prompt #{Shellwords.shellescape(after_prompt)} " +
        "--content #{Shellwords.shellescape(content)} " +
        "#{options.map { |opt| Shellwords.shellescape(opt) }.join(' ')}"
    ret = `#{cmd}`
    ret[-1] == "\n" ? ret.slice(0, ret.length - 1) : ret
  end

end