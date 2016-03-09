require_relative '../bond'
require 'shellwords'

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

  # TODO ETK would be nice if these could also be accessible via command line instead of only through dialogs

  # Display a message to the user using a dialog window. Show some main content
  # (`content`) with shorter strings before (`before_prompt`) and after (`after_prompt`).
  # The content is optional, and only one prompt needs to be specified.
  # `options` is an array of strings which will be displayed to the user
  # as buttons to choose from; the last option is the default. Returns
  # whichever option was chosen (guaranteed to be equal to one of the
  # options).
  bond.spy_point(mock_only: true)
  def self.get_user_input(before_prompt, after_prompt, content, options)
    cmd = "#{Shellwords.shellescape(Bond::BOND_DIALOG_SCRIPT)} " +
        "--before-prompt #{Shellwords.shellescape(before_prompt)} " +
        "--after-prompt #{Shellwords.shellescape(after_prompt)}" +
        "--content #{Shellwords.shellescape(content)}" +
        "#{options.map { |opt| Shellwords.shellescape(opt) }.join(' ')}"
    ret = `#{cmd}`
    ret[-1] == "\n" ? ret.slice(0, ret.length - 1) : ret
  end

  # Same as {Utils.get_user_input} but allows editing of the `content` block.
  # Instead of just returning the option chosen, returns:
  # "option_chosen \n content_including_edits"
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

end